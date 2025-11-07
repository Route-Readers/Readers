package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.MyBook
import com.route.readers.ui.screens.feed.FeedItem
import com.route.readers.ui.screens.profile.Goal
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookRepository = BookRepository()

    private fun getUsersCollection() = firestore.collection("users")

    private fun getMyBooksCollection(userId: String) =
        getUsersCollection().document(userId).collection("myLibrary")

    private fun getReadBooksCollection(userId: String) =
        getUsersCollection().document(userId).collection("readBooks")

    private fun getGoalsCollection(userId: String) =
        getUsersCollection().document(userId).collection("goals")

    suspend fun saveGoal(goal: Goal): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            getGoalsCollection(userId).add(goal).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error saving goal: ${e.message}", e)
            false
        }
    }

    suspend fun getGoals(): List<Goal> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            getGoalsCollection(userId).get().await().toObjects(Goal::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error fetching goals: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            getMyBooksCollection(userId).document(book.isbn).set(book).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding book: ${e.message}", e)
            false
        }
    }

    suspend fun getMyBooks(): List<MyBook> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            getMyBooksCollection(userId).get().await()?.toObjects(MyBook::class.java)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting books: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updateReadingProgress(
        isbn: String,
        currentPage: Int,
        isCompleted: Boolean
    ): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val docRef = getMyBooksCollection(userId).document(isbn)
            val bookSnapshot = docRef.get().await()
            val book = bookSnapshot.toObject(MyBook::class.java)

            if (book != null) {
                val updateData = mutableMapOf<String, Any>(
                    "currentPage" to currentPage,
                    "lastReadDate" to System.currentTimeMillis()
                )

                if (isCompleted) {
                    updateData["isCompleted"] = true
                    val completedDate = System.currentTimeMillis()
                    updateData["completedDate"] = completedDate
                    val completedBook = book.copy(
                        currentPage = currentPage,
                        isCompleted = true,
                        completedDate = completedDate
                    )
                    getReadBooksCollection(userId).document(isbn).set(completedBook).await()
                } else {
                    updateData["isCompleted"] = false
                    updateData["completedDate"] = FieldValue.delete()
                    getReadBooksCollection(userId).document(isbn).delete().await()
                }
                docRef.update(updateData).await()
            }
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error updating progress: ${e.message}", e)
            false
        }
    }

    suspend fun removeBookFromLibrary(isbn: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            getMyBooksCollection(userId).document(isbn).delete().await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error removing book: ${e.message}", e)
            false
        }
    }

    suspend fun markBookAsCompleted(isbn: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val myBookRef = getMyBooksCollection(userId).document(isbn)
            val bookSnapshot = myBookRef.get().await()
            val book = bookSnapshot.toObject(MyBook::class.java)

            if (book != null) {
                val completedDate = System.currentTimeMillis()
                val completedBook = book.copy(isCompleted = true, completedDate = completedDate)

                val readBookRef = getReadBooksCollection(userId).document(isbn)

                firestore.runTransaction { transaction ->
                    transaction.update(
                        myBookRef, mapOf(
                            "isCompleted" to true,
                            "completedDate" to completedDate
                        )
                    )
                    transaction.set(readBookRef, completedBook)
                }.await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error marking book as completed: ${e.message}", e)
            false
        }
    }

    suspend fun markBookAsRead(isbn: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val bookSnapshot = getMyBooksCollection(userId).document(isbn).get().await()
            val myBook = bookSnapshot.toObject(MyBook::class.java)

            if (myBook != null) {
                val readBookRef = getReadBooksCollection(userId).document(isbn)
                val userDocRef = getUsersCollection().document(userId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(readBookRef)
                    if (!snapshot.exists()) {
                        transaction.set(readBookRef, myBook)
                        transaction.update(userDocRef, "readBookCount", FieldValue.increment(1))
                    }
                }.await()
                true
            } else {
                Log.e(
                    "FirestoreRepository",
                    "Failed to get book details from myLibrary for ISBN: $isbn"
                )
                false
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error in markBookAsRead", e)
            false
        }
    }

    suspend fun getReadBooks(userId: String): List<MyBook> {
        return try {
            getReadBooksCollection(userId).get().await().toObjects(MyBook::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error fetching read books", e)
            emptyList()
        }
    }

    private fun getFeedsCollection() = firestore.collection("feeds")

    suspend fun addFeedItem(feedItem: FeedItem): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val user = getUsersCollection().document(userId).get().await()
                .toObject(com.route.readers.data.model.User::class.java)
            val userName = user?.nickname ?: ""

            val itemWithUser = when (feedItem) {
                is FeedItem.BookReview -> feedItem.copy(authorId = userId, userName = userName)
                is FeedItem.FollowNotification -> feedItem.copy(
                    authorId = userId,
                    userName = userName
                )
            }
            getFeedsCollection().add(itemWithUser).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding feed item: ${e.message}", e)
            false
        }
    }

    suspend fun incrementReadBookCount(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            getUsersCollection().document(userId).update("readBookCount", FieldValue.increment(1))
                .await()
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
