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
            val snapshot = docRef.get().await()
            val challenge = snapshot.toObject(Challenge::class.java) ?: return
            
            // 일별 진행도 업데이트
            docRef.update("dailyProgress.$userId.$date", dailyAmount).await()
            
            // 전체 진행도 계산 (일수 기반)
            val dailyProgressMap = challenge.dailyProgress[userId] ?: emptyMap()
            val completedDays = dailyProgressMap.count { it.value >= challenge.goal }
            
            // 전체 진행도 업데이트 (완료한 일수)
            docRef.update("progress.$userId", completedDays).await()
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
    
    suspend fun getAvailableChallenges(): List<Challenge> {
        return try {
            val currentWeek = getCurrentWeekNumber()
            challengesCollection
                .whereEqualTo("weekNumber", currentWeek)
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
