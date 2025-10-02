package com.route.readers.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.MyBook
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private fun getUserBooksCollection() = 
        firestore.collection("users")
            .document(auth.currentUser?.uid ?: "")
            .collection("books")

    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            getUserBooksCollection()
                .document(book.isbn)
                .set(book)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMyBooks(): List<MyBook> {
        return try {
            getUserBooksCollection()
                .get()
                .await()
                .toObjects(MyBook::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateReadingProgress(isbn: String, currentPage: Int): Boolean {
        return try {
            getUserBooksCollection()
                .document(isbn)
                .update(
                    mapOf(
                        "currentPage" to currentPage,
                        "lastReadDate" to System.currentTimeMillis()
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
