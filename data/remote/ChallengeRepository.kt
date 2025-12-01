package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.ChallengeType
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.Calendar

class ChallengeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val challengesCollection = db.collection("challenges")

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getActiveChallengeStream(userId: String): kotlinx.coroutines.flow.Flow<Challenge?> {
        val currentWeek = getCurrentWeekNumber()
        Log.d("ChallengeRepository", "Setting up active challenge listener for userId: $userId, weekNumber: $currentWeek")

        return kotlinx.coroutines.flow.callbackFlow {
            val query = challengesCollection
                .whereArrayContains("participants", userId)
                .whereEqualTo("weekNumber", currentWeek)
                .limit(1)

            val listenerRegistration = query.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w("ChallengeRepository", "Listen failed.", e)
                        close(e)
                        return@addSnapshotListener
                    }

                    val challenge = snapshot?.documents?.firstOrNull()?.toObject(Challenge::class.java)
                    trySend(challenge)
                }

            awaitClose { listenerRegistration.remove() }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    suspend fun getChallenges(): List<Challenge> {
        return try {
            challengesCollection.get().await().documents.mapNotNull { it.toObject(Challenge::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createChallenge(challenge: Challenge) {
        challengesCollection.document(challenge.id).set(challenge).await()
    }

    suspend fun joinChallenge(challengeId: String, userId: String) {
        val docRef = challengesCollection.document(challengeId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            val participants = (snapshot.get("participants") as? List<String> ?: emptyList()).toMutableList()
            if (!participants.contains(userId)) {
                participants.add(userId)
            }

            val progress = (snapshot.get("progress") as? Map<String, Int> ?: emptyMap()).toMutableMap()
            progress[userId] = progress[userId] ?: 0

            transaction.update(docRef, "participants", participants)
            transaction.update(docRef, "progress", progress)
            transaction.update(docRef, "joinDate.$userId", FieldValue.serverTimestamp())

        }.await()
    }

    suspend fun leaveChallenge(challengeId: String, userId: String) {
        val docRef = challengesCollection.document(challengeId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val participants = snapshot.get("participants") as? List<String> ?: emptyList()
            val progress = snapshot.get("progress") as? Map<String, Int> ?: emptyMap()

            val updatedParticipants = participants.filter { it != userId }
            val updatedProgress = progress.toMutableMap().also { it.remove(userId) }

            transaction.update(docRef, mapOf(
                "participants" to updatedParticipants,
                "progress" to updatedProgress
            ))
        }.await()
    }

    suspend fun getUserActiveChallenge(userId: String): Challenge? {
        return try {
            val currentWeek = getCurrentWeekNumber()
            Log.d("ChallengeRepository", "getUserActiveChallenge - userId: $userId, weekNumber: $currentWeek")
            val result = challengesCollection
                .whereArrayContains("participants", userId)
                .whereEqualTo("weekNumber", currentWeek)
                .limit(1)
                .get()
                .await()

            Log.d("ChallengeRepository", "Query result size: ${result.documents.size}")
            result.documents.firstOrNull()?.toObject(Challenge::class.java)
        } catch (e: Exception) {
            Log.e("ChallengeRepository", "Error getting user active challenge", e)
            null
        }
    }

    suspend fun updateDailyProgress(challengeId: String, userId: String, date: String, dailyAmount: Int) {
        val docRef = challengesCollection.document(challengeId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val challenge = snapshot.toObject(Challenge::class.java) ?: return@runTransaction

            val currentDailyProgress = challenge.dailyProgress[userId]?.toMutableMap() ?: mutableMapOf()
            currentDailyProgress[date] = (currentDailyProgress[date] ?: 0) + dailyAmount

            val newProgress = when (challenge.type) {
                ChallengeType.DAILY_PAGES_READING -> {
                    (currentDailyProgress[date] ?: 0)
                }
                ChallengeType.CONSECUTIVE_READING -> {
                    challenge.progress[userId] ?: 0
                }
                else -> challenge.progress[userId] ?: 0
            }

            transaction.update(docRef, "dailyProgress.${userId}.$date", currentDailyProgress[date])
            transaction.update(docRef, "progress.$userId", newProgress)
        }.await()
    }

    private fun getCurrentWeekNumber(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.WEEK_OF_YEAR)
    }

    suspend fun getChallengesForWeek(weekNumber: Int): List<Challenge> {
        return try {
            challengesCollection
                .whereEqualTo("weekNumber", weekNumber)
                .get()
                .await()
                .mapNotNull { it.toObject(Challenge::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getActiveChallenges(userId: String): List<Challenge> {
        return try {
            challengesCollection
                .whereArrayContains("participants", userId)
                .whereGreaterThanOrEqualTo("endDate", System.currentTimeMillis())
                .get()
                .await()
                .mapNotNull { it.toObject(Challenge::class.java) }
        } catch (e: Exception) {
            Log.e("ChallengeRepository", "Error getting active challenges", e)
            emptyList()
        }
    }
}

