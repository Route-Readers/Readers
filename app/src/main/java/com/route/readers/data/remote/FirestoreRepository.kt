package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.MyBook
import com.route.readers.ui.screens.feed.FeedItem
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getFeedsCollection() = firestore.collection("feeds")

    suspend fun addFeedItem(feedItem: FeedItem): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val user = firestore.collection("users").document(userId).get().await().toObject(com.route.readers.data.model.User::class.java)
            val userName = user?.nickname ?: ""

            val itemWithUser = when (feedItem) {
                is FeedItem.BookReview -> feedItem.copy(authorId = userId, userName = userName)
                is FeedItem.FollowNotification -> feedItem.copy(authorId = userId, userName = userName)
            }

            getFeedsCollection().add(itemWithUser).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding feed item: ${e.message}", e)
            false
        }
    }

    private fun getMyBooksCollection() = firestore.collection("my_books")

    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            Log.d("FirestoreRepository", "Adding book: ${book.title} for user: $userId")
            
            val bookData = book.copy(userId = userId)
            
            getMyBooksCollection()
                .document("${userId}_${book.isbn}")
                .set(bookData)
                .await()
            Log.d("FirestoreRepository", "Book added successfully")
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding book: ${e.message}", e)
            false
        }
    }

    suspend fun getMyBooks(): List<MyBook> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            Log.d("FirestoreRepository", "Getting books for user: $userId")
            
            val result = getMyBooksCollection()
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(MyBook::class.java)
            Log.d("FirestoreRepository", "Got ${result.size} books")
            result
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting books: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updateReadingProgress(isbn: String, currentPage: Int): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            Log.d("FirestoreRepository", "Updating progress for $isbn to page $currentPage")
            
            // 먼저 해당 책이 존재하는지 확인
            val docRef = getMyBooksCollection().document("${userId}_${isbn}")
            val document = docRef.get().await()
            
            if (!document.exists()) {
                Log.e("FirestoreRepository", "Book not found: ${userId}_${isbn}")
                return false
            }
            
            val totalPages = document.getLong("totalPages")?.toInt() ?: 0
            val isCompleted = if (totalPages > 0) currentPage >= totalPages else false
            
            val updateData = mutableMapOf<String, Any>(
                "currentPage" to currentPage,
                "lastReadDate" to System.currentTimeMillis(),
                "isCompleted" to isCompleted
            )
            
            if (isCompleted) {
                updateData["completedDate"] = System.currentTimeMillis()
            }
            
            docRef.update(updateData).await()
            Log.d("FirestoreRepository", "Progress updated successfully. Completed: $isCompleted")
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error updating progress: ${e.message}", e)
            false
        }
    }

    suspend fun removeBookFromLibrary(isbn: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            Log.d("FirestoreRepository", "Removing book: $isbn for user: $userId")
            
            getMyBooksCollection()
                .document("${userId}_${isbn}")
                .delete()
                .await()
            Log.d("FirestoreRepository", "Book removed successfully")
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error removing book: ${e.message}", e)
            false
        }
    }

    suspend fun markBookAsCompleted(isbn: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            Log.d("FirestoreRepository", "Marking book as completed: $isbn")
            
            getMyBooksCollection()
                .document("${userId}_${isbn}")
                .update(
                    mapOf(
                        "isCompleted" to true,
                        "completedDate" to System.currentTimeMillis()
                    )
                )
                .await()
            Log.d("FirestoreRepository", "Book marked as completed")
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error marking book as completed: ${e.message}", e)
            false
        }
    }

    suspend fun testFirestoreConnection(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            val testData = mapOf(
                "test" to "connection",
                "userId" to userId,
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore.collection("test_connections")
                .document("test_${userId}_${System.currentTimeMillis()}")
                .set(testData)
                .await()
            
            Log.d("FirestoreRepository", "Test connection successful")
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Test connection failed: ${e.message}", e)
            false
        }
    }
}
