package com.route.readers.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.UsedBook
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UsedBookRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usedBooksCollection = db.collection("usedBooks")

    fun getUsedBooksFlow(): Flow<List<UsedBook>> = callbackFlow {
        val listener = usedBooksCollection
            .whereEqualTo("status", "판매중")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("UsedBookRepository", "Listen failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val books = snapshot?.documents?.mapNotNull { 
                    it.toObject(UsedBook::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                
                android.util.Log.d("UsedBookRepository", "Real-time update: ${books.size} books")
                trySend(books)
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun getUsedBooks(): List<UsedBook> {
        return try {
            android.util.Log.d("UsedBookRepository", "getUsedBooks started")
            val result = usedBooksCollection
                .whereEqualTo("status", "판매중")
                .get()
                .await()
            
            android.util.Log.d("UsedBookRepository", "Found ${result.documents.size} books")
            val books = result.documents.mapNotNull { 
                it.toObject(UsedBook::class.java)?.copy(id = it.id)
            }
            books.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            android.util.Log.e("UsedBookRepository", "Error getting books", e)
            emptyList()
        }
    }

    suspend fun createUsedBook(usedBook: UsedBook) {
        try {
            android.util.Log.d("UsedBookRepository", "Creating book: ${usedBook.id}")
            usedBooksCollection.document(usedBook.id).set(usedBook).await()
            android.util.Log.d("UsedBookRepository", "Book created successfully")
        } catch (e: Exception) {
            android.util.Log.e("UsedBookRepository", "Error creating book", e)
            e.printStackTrace()
        }
    }

    suspend fun getUsedBook(bookId: String): UsedBook? {
        return try {
            usedBooksCollection.document(bookId).get().await().toObject(UsedBook::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteUsedBook(bookId: String) {
        try {
            usedBooksCollection.document(bookId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUsedBook(usedBook: UsedBook) {
        try {
            usedBooksCollection.document(usedBook.id).set(usedBook).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateBookStatus(bookId: String, status: String, buyerId: String? = null) {
        try {
            val updates = mutableMapOf<String, Any>("status" to status)
            buyerId?.let { updates["buyerId"] = it }
            usedBooksCollection.document(bookId).update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
