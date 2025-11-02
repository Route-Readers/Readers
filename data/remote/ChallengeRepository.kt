package com.route.readers.data.remote

import com.google.firebase.firestore.FirebaseFirestore
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
        challengesCollection.document(challenge.id).set(challenge).await()
    }

    suspend fun joinChallenge(challengeId: String, userId: String) {
        val docRef = challengesCollection.document(challengeId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val participants = snapshot.get("participants") as? List<*> ?: emptyList<String>()
            val progress = snapshot.get("progress") as? Map<*, *> ?: emptyMap<String, Int>()
            
            transaction.update(docRef, mapOf(
                "participants" to participants + userId,
                "progress" to progress + (userId to 0)
            ))
        }.await()
    }
}
