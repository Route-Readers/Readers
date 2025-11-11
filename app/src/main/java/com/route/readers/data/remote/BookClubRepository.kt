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

    // 실시간 북클럽 목록 가져오기
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
            // 먼저 현재 북클럽 정보 확인
            val bookClubDoc = firestore.collection("bookClubs").document(bookClubId).get().await()
            val bookClub = bookClubDoc.toObject(BookClub::class.java)
            
            if (bookClub != null && !bookClub.members.contains(userId)) {
                firestore.collection("bookClubs").document(bookClubId)
                    .update(
                        "members", FieldValue.arrayUnion(userId),
                        "memberCount", FieldValue.increment(1)
                    ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveBookClub(bookClubId: String, userId: String): Result<Unit> {
        return try {
            // 먼저 현재 북클럽 정보 확인
            val bookClubDoc = firestore.collection("bookClubs").document(bookClubId).get().await()
            val bookClub = bookClubDoc.toObject(BookClub::class.java)
            
            if (bookClub != null && bookClub.members.contains(userId)) {
                firestore.collection("bookClubs").document(bookClubId)
                    .update(
                        "members", FieldValue.arrayRemove(userId),
                        "memberCount", FieldValue.increment(-1)
                    ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBookClub(bookClubId: String, userId: String): Result<Unit> {
        return try {
            val bookClubDoc = firestore.collection("bookClubs").document(bookClubId).get().await()
            val bookClub = bookClubDoc.toObject(BookClub::class.java)
            
            if (bookClub?.createdBy == userId) {
                // 채팅 메시지들도 함께 삭제
                val messagesSnapshot = firestore.collection("bookClubs")
                    .document(bookClubId)
                    .collection("messages")
                    .get().await()
                
                messagesSnapshot.documents.forEach { messageDoc ->
                    messageDoc.reference.delete()
                }
                
                // 북클럽 삭제
                firestore.collection("bookClubs").document(bookClubId).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
