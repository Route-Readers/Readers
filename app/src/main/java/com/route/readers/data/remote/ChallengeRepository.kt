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
            val docRef = challengesCollection.document(challengeId)
            val snapshot = docRef.get().await()
            
            // 문서가 존재하지 않으면 생성
            if (!snapshot.exists()) {
                return
            }
            
            // 이미 참여 중인지 확인
            val participants = snapshot.get("participants") as? List<*> ?: emptyList<String>()
            if (participants.contains(userId)) {
                return
            }
            
            // 참여자 추가 및 진행도 초기화
            docRef.update(
                mapOf(
                    "participants" to FieldValue.arrayUnion(userId),
                    "progress.$userId" to 0
                )
            ).await()
        } catch (e: Exception) {
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
}
