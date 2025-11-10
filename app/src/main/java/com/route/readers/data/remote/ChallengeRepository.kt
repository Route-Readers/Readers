package com.route.readers.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.route.readers.data.model.Challenge
import kotlinx.coroutines.tasks.await

class ChallengeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val challengesCollection = db.collection("challenges")

    suspend fun getChallenges(): List<Challenge> {
        return try {
            challengesCollection.get().await().documents.mapNotNull { it.toObject(Challenge::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createChallenge(challenge: Challenge) {
        try {
            challengesCollection.document(challenge.id).set(challenge).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun joinChallenge(challengeId: String, userId: String) {
        try {
            android.util.Log.d("ChallengeRepository", "joinChallenge - challengeId: $challengeId, userId: $userId")

            val docRef = challengesCollection.document(challengeId)
            val snapshot = docRef.get().await()

            // 문서가 존재하지 않으면 생성
            if (!snapshot.exists()) {
                android.util.Log.e("ChallengeRepository", "Challenge document not found: $challengeId")
                return
            }

            // 이미 참여 중인지 확인
            val participants = snapshot.get("participants") as? List<*> ?: emptyList<String>()
            android.util.Log.d("ChallengeRepository", "Current participants: $participants")

            if (participants.contains(userId)) {
                android.util.Log.d("ChallengeRepository", "User already participating")
                return
            }

            // 참여자 추가 및 진행도 초기화
            docRef.update(
                mapOf(
                    "participants" to FieldValue.arrayUnion(userId),
                    "progress.$userId" to 0
                )
            ).await()

            android.util.Log.d("ChallengeRepository", "Successfully joined challenge")
        } catch (e: Exception) {
            android.util.Log.e("ChallengeRepository", "Error joining challenge", e)
            e.printStackTrace()
        }
    }

    suspend fun updateProgress(challengeId: String, userId: String, progress: Int) {
        try {
            challengesCollection.document(challengeId)
                .update("progress.$userId", progress)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateDailyProgress(challengeId: String, userId: String, date: String, dailyAmount: Int) {
        try {
            val docRef = challengesCollection.document(challengeId)

            // 트랜잭션을 사용하여 데이터 일관성 보장
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val challenge = snapshot.toObject(Challenge::class.java) ?: return@runTransaction

                // 1. 일별 진행도(읽은 페이지 수) 업데이트
                transaction.update(docRef, "dailyProgress.$userId.$date", dailyAmount)

                // 2. 챌린지 타입에 따라 전체 진행도(달성 일수) 계산
                // 트랜잭션 내에서 업데이트된 값을 포함하여 다시 계산하기 위해 snapshot에서 데이터를 가져옴
                val dailyProgressData = snapshot.get("dailyProgress.$userId") as? Map<String, Long> ?: emptyMap()
                // 현재 업데이트를 반영하기 위해 맵을 새로 만듬
                val updatedDailyProgress = dailyProgressData.toMutableMap()
                updatedDailyProgress[date] = dailyAmount.toLong()

                val newProgress = when (challenge.type) {
                    com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING -> {
                        // 목표 페이지(goal) 이상 읽은 날의 수를 계산
                        updatedDailyProgress.count { it.value >= challenge.goal }.coerceAtMost(7)
                    }
                    com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING -> {
                        // 1페이지 이상 읽은 날의 수를 계산
                        updatedDailyProgress.count { it.value > 0 }.coerceAtMost(7)
                    }
                    else -> {
                        // 다른 타입의 챌린지는 일단 기존 값을 유지
                        challenge.progress[userId] ?: 0
                    }
                }

                // 3. 계산된 전체 진행도로 업데이트
                transaction.update(docRef, "progress.$userId", newProgress)
            }.await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserActiveChallenge(userId: String): Challenge? {
        return try {
            val currentWeek = getCurrentWeekNumber()
            android.util.Log.d("ChallengeRepository", "getUserActiveChallenge - userId: $userId, weekNumber: $currentWeek")

            val result = challengesCollection
                .whereArrayContains("participants", userId)
                .whereEqualTo("weekNumber", currentWeek)
                .get()
                .await()

            android.util.Log.d("ChallengeRepository", "Query result size: ${result.documents.size}")
            result.documents.forEach { doc ->
                android.util.Log.d("ChallengeRepository", "Document: ${doc.id}, participants: ${doc.get("participants")}, weekNumber: ${doc.get("weekNumber")}")
            }

            result.documents.firstOrNull()?.toObject(Challenge::class.java)
        } catch (e: Exception) {
            android.util.Log.e("ChallengeRepository", "Error getting user active challenge", e)
            null
        }
    }

    suspend fun getChallengesForWeek(weekNumber: Int): List<Challenge> {
        return try {
            challengesCollection
                .whereEqualTo("weekNumber", weekNumber)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Challenge::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getCurrentWeekNumber(): Int {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val week = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        return year * 100 + week
    }

    suspend fun leaveChallenge(challengeId: String, userId: String) {
        try {
            val docRef = challengesCollection.document(challengeId)
            docRef.update(
                mapOf(
                    "participants" to FieldValue.arrayRemove(userId),
                    "progress.$userId" to FieldValue.delete()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}