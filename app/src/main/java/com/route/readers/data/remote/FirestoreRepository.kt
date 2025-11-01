package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.ui.screens.feed.FeedItem
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookRepository = BookRepository()

    private fun getFeedsCollection() = firestore.collection("feeds")
    private fun getUsersCollection() = firestore.collection("users")

    suspend fun addFeedItem(feedItem: FeedItem): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val user = getUsersCollection().document(userId).get().await()
                .toObject(com.route.readers.data.model.User::class.java)
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

    private fun getMyBooksCollection() =
        auth.currentUser?.uid?.let { getUsersCollection().document(it).collection("myLibrary") }

    private fun getReadBooksCollection() =
        auth.currentUser?.uid?.let { getUsersCollection().document(it).collection("readBooks") }

    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            getMyBooksCollection()?.document(book.isbn)?.set(book)?.await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding book: ${e.message}", e)
            false
        }
    }

    suspend fun getMyBooks(): List<MyBook> {
        return try {
            getMyBooksCollection()?.get()?.await()?.toObjects(MyBook::class.java) ?: emptyList()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting books: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updateReadingProgress(isbn: String, currentPage: Int, isCompleted: Boolean): Boolean {
        return try {
            val docRef = getMyBooksCollection()?.document(isbn) ?: return false
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
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error updating progress: ${e.message}", e)
            false
        }
    }

    suspend fun removeBookFromLibrary(isbn: String): Boolean {
        return try {
            getMyBooksCollection()?.document(isbn)?.delete()?.await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error removing book: ${e.message}", e)
            false
        }
    }

    suspend fun markBookAsCompleted(isbn: String): Boolean {
        return try {
            getMyBooksCollection()?.document(isbn)?.update(
                mapOf(
                    "isCompleted" to true,
                    "completedDate" to System.currentTimeMillis()
                )
            )?.await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error marking book as completed: ${e.message}", e)
            false
        }
    }

    suspend fun markBookAsRead(isbn: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val fullBookDetail = bookRepository.getBookDetail(isbn)

            if (fullBookDetail != null) {
                val readBookDocRef = getReadBooksCollection()?.document(isbn)
                val userDocRef = getUsersCollection().document(userId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(readBookDocRef!!)
                    if (!snapshot.exists()) {
                        transaction.set(readBookDocRef, fullBookDetail)
                        transaction.update(userDocRef, "readBookCount", FieldValue.increment(1))
                    }
                }.await()
                true
            } else {
                Log.e("FirestoreRepository", "Failed to get book details for ISBN: $isbn")
                false
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error marking book as read", e)
            false
        }
    }


    suspend fun getReadBooks(): List<Book> {
        return try {
            getReadBooksCollection()?.get()?.await()?.toObjects(Book::class.java) ?: emptyList()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error fetching read books", e)
            emptyList()
        }
    }

    suspend fun incrementReadBookCount(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            getUsersCollection().document(userId).update("readBookCount", FieldValue.increment(1)).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error incrementing read book count", e)
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
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Test connection failed: ${e.message}", e)
            false
        }
    }
}
