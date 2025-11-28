package com.route.readers.data.remote

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

class ChallengeRepository {
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

            transaction.update(docRef, mapOf(
                "participants" to updatedParticipants,
                "progress" to updatedProgress
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
            android.util.Log.d("ChallengeRepository", "getUserActiveChallenge - userId: $userId, weekNumber: $currentWeek")
            val result = challengesCollection
                .whereArrayContains("participants", userId)
                .whereEqualTo("weekNumber", currentWeek)
                .limit(1)
                .get()
                .await()

            android.util.Log.d("ChallengeRepository", "Query result size: ${result.documents.size}")
            result.documents.firstOrNull()?.toObject(Challenge::class.java)
        } catch (e: Exception) {
            android.util.Log.e("ChallengeRepository", "Error getting user active challenge", e)
            null
        }
    }

    suspend fun updateDailyProgress(challengeId: String, userId: String, date: String, dailyAmount: Int) {
        android.util.Log.d("ChallengeRepo", "updateDailyProgress called for challenge: $challengeId, user: $userId, date: $date, amount: $dailyAmount")
        val docRef = challengesCollection.document(challengeId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val challenge = snapshot.toObject(Challenge::class.java) ?: run {
                android.util.Log.e("ChallengeRepo", "Challenge $challengeId not found for updateDailyProgress")
                return@runTransaction
            }
            android.util.Log.d("ChallengeRepo", "Before update - Challenge $challengeId, user $userId progress: ${challenge.progress[userId]}, dailyProgress: ${challenge.dailyProgress[userId]?.get(date)}")

            val currentDailyProgress = challenge.dailyProgress[userId]?.toMutableMap() ?: mutableMapOf()
            currentDailyProgress[date] = (currentDailyProgress[date] ?: 0) + dailyAmount

            val newProgress = when (challenge.type) {
                ChallengeType.DAILY_PAGES_READING -> {
                    (currentDailyProgress[date] ?: 0)
                }
                ChallengeType.CONSECUTIVE_READING -> {
                    // For consecutive reading, progress is usually 1 (day counted) or 0.
                    // This logic might need adjustment if it's meant to track a streak.
                    (challenge.progress[userId] ?: 0) + 1 // Assuming progress here means days, not amount
                }
                else -> challenge.progress[userId] ?: 0
            }

            // Corrected lines
            transaction.update(docRef, mapOf(
                "dailyProgress.${userId}.${date}" to (currentDailyProgress[date] ?: 0),
                "progress.${userId}" to newProgress
            ))
            android.util.Log.d("ChallengeRepo", "After update - Challenge $challengeId, user $userId new progress: $newProgress, new dailyProgress: ${currentDailyProgress[date]}")

        }.await()
        refreshUserActiveChallenge(userId)
        android.util.Log.d("ChallengeRepo", "updateDailyProgress transaction completed and refresh triggered for user: $userId")
    }

    suspend fun updatePagesReadChallengeProgress(userId: String, pagesRead: Int) {
        android.util.Log.d("ChallengeRepo", "updatePagesReadChallengeProgress called for user: $userId, pagesRead: $pagesRead")
        val challengesToUpdate = getUserActiveDailyPageChallenges(userId)
        if (challengesToUpdate.isEmpty()) {
            android.util.Log.d("ChallengeRepo", "No active daily page challenges found for user: $userId")
        }
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        challengesToUpdate.forEach { challenge ->
            android.util.Log.d("ChallengeRepo", "Updating daily progress for challenge: ${challenge.id}, type: ${challenge.type}")
            updateDailyProgress(challenge.id, userId, today, pagesRead)
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
            android.util.Log.e("ChallengeRepository", "Error getting user challenges", e)
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
            android.util.Log.e("ChallengeRepository", "Error getting user active daily page challenges", e)
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
            android.util.Log.e("ChallengeRepository", "Error getting active challenges", e)
            emptyList()
        }
    }
}
