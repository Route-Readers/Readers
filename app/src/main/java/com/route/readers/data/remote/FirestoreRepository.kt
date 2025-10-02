package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.MyBook
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    private fun getUserBooksCollection() = 
        firestore.collection("users")
            .document("test_user") // 테스트용 고정 사용자 ID
            .collection("books")

    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            Log.d("FirestoreRepository", "Adding book: ${book.title}")
            getUserBooksCollection()
                .document(book.isbn)
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
            Log.d("FirestoreRepository", "Getting books...")
            val result = getUserBooksCollection()
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
            Log.d("FirestoreRepository", "Updating progress for $isbn to page $currentPage")
            getUserBooksCollection()
                .document(isbn)
                .update(
                    mapOf(
                        "currentPage" to currentPage,
                        "lastReadDate" to System.currentTimeMillis()
                    )
                )
                .await()
            Log.d("FirestoreRepository", "Progress updated successfully")
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error updating progress: ${e.message}", e)
            false
        }
    }

    suspend fun removeBookFromLibrary(isbn: String): Boolean {
        return try {
            Log.d("FirestoreRepository", "Removing book: $isbn")
            getUserBooksCollection()
                .document(isbn)
                .delete()
                .await()
            Log.d("FirestoreRepository", "Book removed successfully")
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error removing book: ${e.message}", e)
            false
        }
    }
}
