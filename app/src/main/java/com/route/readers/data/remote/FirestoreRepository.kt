package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.route.readers.data.model.MyBook
import com.route.readers.data.model.ReadingSession
import com.route.readers.data.model.User
import com.route.readers.ui.screens.feed.FeedItem
import com.route.readers.ui.screens.profile.Goal
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookRepository = BookRepository()
    private val challengeRepository = ChallengeRepository(this)

    private fun getUsersCollection() = firestore.collection("users")

    private fun getMyBooksCollection(userId: String) =
        getUsersCollection().document(userId).collection("myLibrary")

    private fun getReadBooksCollection(userId: String) =
        getUsersCollection().document(userId).collection("readBooks")

    private fun getGoalsCollection(userId: String) =
        getUsersCollection().document(userId).collection("goals")

    suspend fun getUserProfile(userId: String): User? {
        return try {
            val doc = getUsersCollection().document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting user profile: ${e.message}", e)
            null
        }
    }

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

    suspend fun getPagesReadToday(): Int {
        val userId = auth.currentUser?.uid ?: return 0
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val doc = getUsersCollection().document(userId)
                .collection("daily_reading").document(today)
                .get().await()
            doc.getLong("pagesRead")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting pages read today", e)
            0
        }
    }

    suspend fun getPagesReadOnDate(userId: String, date: String): Int {
        return try {
            val doc = getUsersCollection().document(userId)
                .collection("daily_reading").document(date)
                .get().await()
            doc.getLong("pagesRead")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting pages read on date $date: ${e.message}", e)
            0
        }
    }

    suspend fun getDailyReadings(userId: String): Map<String, Int> {
        return try {
            val snapshot = getUsersCollection().document(userId)
                .collection("daily_reading")
                .get().await()
            snapshot.documents.associate { doc ->
                doc.id to (doc.getLong("pagesRead")?.toInt() ?: 0)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting daily readings", e)
            emptyMap()
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

    suspend fun deleteGoal(bookIsbn: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val querySnapshot = getGoalsCollection(userId)
                .whereEqualTo("bookIsbn", bookIsbn)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val documentToDelete = querySnapshot.documents[0]
                documentToDelete.reference.delete().await()
            }
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error deleting goal", e)
            false
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

    suspend fun getMyBooks(source: com.google.firebase.firestore.Source = com.google.firebase.firestore.Source.DEFAULT): List<MyBook> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            getMyBooksCollection(userId).get(source).await()?.toObjects(MyBook::class.java)
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
                val oldCurrentPage = book.currentPage
                val pagesReadThisSession = currentPage - oldCurrentPage

                if (pagesReadThisSession > 0) {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val dailyReadingRef = getUsersCollection().document(userId)
                        .collection("daily_reading").document(today)
                    dailyReadingRef.set(
                        mapOf("pagesRead" to FieldValue.increment(pagesReadThisSession.toLong())),
                        SetOptions.merge()
                    ).await()

                    val userRef = getUsersCollection().document(userId)
                    userRef.update("totalPagesRead", FieldValue.increment(pagesReadThisSession.toLong())).await()

                    challengeRepository.updatePagesReadChallengeProgress(userId)
                }

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
                .toObject(User::class.java)
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

    suspend fun addReadingTime(bookId: String, timeInSeconds: Int): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val docRef = getMyBooksCollection(userId).document(bookId)
            docRef.update("totalReadingTime", FieldValue.increment(timeInSeconds.toLong())).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error updating reading time: ${e.message}", e)
            false
        }
    }

    suspend fun addReadingSession(session: ReadingSession): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            getUsersCollection().document(userId)
                .collection("reading_sessions")
                .add(session)
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding reading session: ${e.message}", e)
            false
        }
    }

    suspend fun getReadingSessions(userId: String, startDate: Date?, endDate: Date?): List<ReadingSession> {
        return try {
            var query = getUsersCollection().document(userId)
                .collection("reading_sessions")
                .orderBy("startTime")

            if (startDate != null) {
                query = query.whereGreaterThanOrEqualTo("startTime", startDate)
            }
            if (endDate != null) {
                query = query.whereLessThanOrEqualTo("startTime", endDate)
            }

            query.get().await().documents.mapNotNull { it.toObject(ReadingSession::class.java) }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting reading sessions: ${e.message}", e)
            emptyList()
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

    suspend fun getDailyPagesRead(userId: String, date: String): Int {
        return try {
            val dailyReadingRef = getUsersCollection().document(userId)
                .collection("daily_reading").document(date)
            val snapshot = dailyReadingRef.get().await()
            snapshot.getLong("pagesRead")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting daily pages read: ${e.message}", e)
            0
        }
    }

    suspend fun addExp(amount: Int): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val userRef = getUsersCollection().document(userId)
            userRef.update("xp", FieldValue.increment(amount.toLong())).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding EXP", e)
            false
        }
    }
}
