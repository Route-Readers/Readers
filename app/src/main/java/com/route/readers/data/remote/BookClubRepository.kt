package com.route.readers.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BookClubRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val firestoreRepository = FirestoreRepository()

    fun getAllBookClubsFlow(): Flow<List<BookClub>> = callbackFlow {
        val listener = firestore.collection("bookClubs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val bookClubs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BookClub::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(bookClubs)
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun getAllBookClubs(): Result<List<BookClub>> {
        return try {
            val snapshot = firestore.collection("bookClubs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val bookClubs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BookClub::class.java)?.copy(id = doc.id)
            }
            Result.success(bookClubs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookClubById(bookClubId: String): Result<BookClub?> {
        return try {
            val doc = firestore.collection("bookClubs").document(bookClubId).get().await()
            val bookClub = doc.toObject(BookClub::class.java)?.copy(id = doc.id)
            Result.success(bookClub)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBookClub(bookClub: BookClub): Result<String> {
        return try {
            val docRef = firestore.collection("bookClubs").add(bookClub).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinBookClub(bookClubId: String, userId: String): Result<Unit> {
        return try {
            val bookClubDoc = firestore.collection("bookClubs").document(bookClubId).get().await()
            val bookClub = bookClubDoc.toObject(BookClub::class.java)
            
            if (bookClub != null && !bookClub.members.contains(userId)) {
                firestore.collection("bookClubs").document(bookClubId)
                    .update(
                        "members", FieldValue.arrayUnion(userId),
                        "memberCount", FieldValue.increment(1)
                    ).await()
                
                // 참여 시스템 메시지 전송
                sendJoinMessage(bookClubId, userId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun sendJoinMessage(bookClubId: String, userId: String) {
        try {
            val user = firestoreRepository.getUserProfile(userId)
            val nickname = user?.nickname ?: "알 수 없음"
            
            val systemMessage = ChatMessage(
                bookClubId = bookClubId,
                senderId = "system",
                senderName = "시스템",
                senderProfileImage = "",
                message = "${nickname}님이 북클럽에 참여했습니다.",
                timestamp = System.currentTimeMillis()
            )
            
            firestore.collection("bookClubs")
                .document(bookClubId)
                .collection("messages")
                .add(systemMessage)
                .await()
        } catch (e: Exception) {
            // 시스템 메시지 실패해도 참여는 성공
        }
    }

    suspend fun leaveBookClub(bookClubId: String, userId: String): Result<Unit> {
        return try {
            val bookClubDoc = firestore.collection("bookClubs").document(bookClubId).get().await()
            val bookClub = bookClubDoc.toObject(BookClub::class.java)
            
            if (bookClub != null && bookClub.members.contains(userId)) {
                // 나가기 시스템 메시지 먼저 전송
                sendLeaveMessage(bookClubId, userId)
                
                firestore.collection("bookClubs").document(bookClubId)
                    .update(
                        "members", FieldValue.arrayRemove(userId),
                        "viceOwners", FieldValue.arrayRemove(userId),
                        "memberCount", FieldValue.increment(-1)
                    ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun sendLeaveMessage(bookClubId: String, userId: String) {
        try {
            val user = firestoreRepository.getUserProfile(userId)
            val nickname = user?.nickname ?: "알 수 없음"
            
            val systemMessage = ChatMessage(
                bookClubId = bookClubId,
                senderId = "system",
                senderName = "시스템",
                senderProfileImage = "",
                message = "${nickname}님이 북클럽을 나갔습니다.",
                timestamp = System.currentTimeMillis()
            )
            
            firestore.collection("bookClubs")
                .document(bookClubId)
                .collection("messages")
                .add(systemMessage)
                .await()
        } catch (e: Exception) {
            // 시스템 메시지 실패해도 나가기는 성공
        }
    }

    suspend fun deleteBookClub(bookClubId: String, userId: String): Result<Unit> {
        return try {
            val bookClubDoc = firestore.collection("bookClubs").document(bookClubId).get().await()
            val bookClub = bookClubDoc.toObject(BookClub::class.java)
            
            if (bookClub?.createdBy == userId) {
                val messagesSnapshot = firestore.collection("bookClubs")
                    .document(bookClubId)
                    .collection("messages")
                    .get().await()
                
                messagesSnapshot.documents.forEach { messageDoc ->
                    messageDoc.reference.delete()
                }
                
                firestore.collection("bookClubs").document(bookClubId).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
