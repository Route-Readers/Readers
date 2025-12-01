package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.ChallengeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class ChallengeRepository(
    private val firestoreRepository: FirestoreRepository
) {
    private val db = FirebaseFirestore.getInstance()
    private val challengesCollection = db.collection("challenges")

    private val _userActiveChallenges = MutableStateFlow<Map<String, Challenge?>>(emptyMap())
    val userActiveChallenges: StateFlow<Map<String, Challenge?>> = _userActiveChallenges.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun refreshUserActiveChallenge(userId: String) {
        repositoryScope.launch {
            val activeChallenge = getUserActiveChallenge(userId)
            _userActiveChallenges.value = _userActiveChallenges.value + (userId to activeChallenge)
        }
    }

    suspend fun getChallenges(): List<Challenge> {
        return try {
            challengesCollection.get().await().documents.mapNotNull { it.toObject(Challenge::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getChallenge(challengeId: String): Challenge? {
        return try {
            challengesCollection.document(challengeId).get().await().toObject(Challenge::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createChallenge(challenge: Challenge) {
        challengesCollection.document(challenge.id).set(challenge).await()
    }

    suspend fun joinChallenge(challengeId: String, userId: String) {
        val docRef = challengesCollection.document(challengeId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val participants = snapshot.get("participants") as? List<*> ?: emptyList<String>()
            val progress = snapshot.get("progress") as? Map<*, *> ?: emptyMap<String, Int>()

            val updatedParticipants = participants + userId

            // 수정된 부분: 새로운 참여자의 진행률을 0으로 초기화합니다.
            val updatedProgress = progress.toMutableMap().apply {
                this[userId] = 0
            }

            val currentJoinDates = snapshot.get("joinDate") as? Map<String, java.util.Date> ?: emptyMap()
            val updatedJoinDates = currentJoinDates.toMutableMap().apply {
                this[userId] = java.util.Date() // Firestore will convert this to a ServerTimestamp when committed.
            }

            transaction.update(docRef, mapOf(
                "participants" to updatedParticipants,
                "progress" to updatedProgress,
                "joinDate" to updatedJoinDates
            ))
        }.await()

        val updatedChallenge = challengesCollection.document(challengeId).get().await().toObject(Challenge::class.java)
        if (updatedChallenge != null) {
            _userActiveChallenges.value = _userActiveChallenges.value + (userId to updatedChallenge)
        }
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
        refreshUserActiveChallenge(userId)
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

    suspend fun updateDailyProgress(challengeId: String, userId: String, date: String) {
        val docRef = challengesCollection.document(challengeId)

        // Fetch the actual accumulated pages for the day from FirestoreRepository BEFORE the transaction
        val accumulatedPagesToday = firestoreRepository.getPagesReadOnDate(userId, date)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val challenge = snapshot.toObject(Challenge::class.java) ?: return@runTransaction

            val currentDailyProgress = challenge.dailyProgress[userId]?.toMutableMap() ?: mutableMapOf()
            currentDailyProgress[date] = accumulatedPagesToday // Use the pre-fetched value

            val newProgress = when (challenge.type) {
                ChallengeType.DAILY_PAGES_READING -> {
                    accumulatedPagesToday // Use the pre-fetched value directly
                }
                ChallengeType.CONSECUTIVE_READING -> {
                    challenge.progress[userId] ?: 0
                }
                else -> challenge.progress[userId] ?: 0
            }

            transaction.update(docRef, mapOf(
                "dailyProgress.${userId}.${date}" to accumulatedPagesToday,
                "progress.${userId}" to newProgress
            ))
        }.await()
        refreshUserActiveChallenge(userId)
    }

    suspend fun updatePagesReadChallengeProgress(userId: String) { // Removed pagesRead parameter
        val challengesToUpdate = getUserActiveDailyPageChallenges(userId)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        challengesToUpdate.forEach { challenge ->
            updateDailyProgress(challenge.id, userId, today) // Call without dailyAmount
        }
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

    suspend fun getUserChallenges(userId: String): List<Challenge> {
        return try {
            challengesCollection
                .whereArrayContains("participants", userId)
                .get()
                .await()
                .mapNotNull { it.toObject(Challenge::class.java) }
        } catch (e: Exception) {
            Log.e("ChallengeRepository", "Error getting user challenges", e)
            emptyList()
        }
    }

    suspend fun getUserActiveDailyPageChallenges(userId: String): List<Challenge> {
        return try {
            challengesCollection
                .whereArrayContains("participants", userId)
                .whereEqualTo("type", ChallengeType.DAILY_PAGES_READING)
                .whereGreaterThanOrEqualTo("endDate", System.currentTimeMillis())
                .get()
                .await()
                .mapNotNull { it.toObject(Challenge::class.java) }
        } catch (e: Exception) {
            Log.e("ChallengeRepository", "Error getting user active daily page challenges", e)
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