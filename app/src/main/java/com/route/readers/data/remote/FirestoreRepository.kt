package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

    private fun getMyBooksCollection() = firestore.collection("users")

    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            Log.d("FirestoreRepository", "Adding book: ${book.title} for user: $userId")

            getMyBooksCollection().document(userId)
                .collection("myLibrary").document(book.isbn)
                .set(book)
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

            val result = getMyBooksCollection().document(userId)
                .collection("myLibrary")
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

    suspend fun updateReadingProgress(isbn: String, currentPage: Int, isCompleted: Boolean): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            Log.d("FirestoreRepository", "Updating progress for $isbn to page $currentPage. Completed: $isCompleted")

            val docRef = getMyBooksCollection().document(userId)
                .collection("myLibrary").document(isbn)

            val updateData = mutableMapOf<String, Any>(
                "currentPage" to currentPage,
                "lastReadDate" to System.currentTimeMillis()
            )

            if (isCompleted) {
                updateData["isCompleted"] = true
                updateData["completedDate"] = System.currentTimeMillis()
            } else {
                updateData["isCompleted"] = false
                updateData["completedDate"] = FieldValue.delete()
            }


            docRef.update(updateData).await()
            Log.d("FirestoreRepository", "Progress updated successfully.")
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

            getMyBooksCollection().document(userId)
                .collection("myLibrary").document(isbn)
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

            getMyBooksCollection().document(userId)
                .collection("myLibrary").document(isbn)
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
