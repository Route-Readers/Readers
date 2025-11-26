package com.route.readers.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.Book
import kotlinx.coroutines.tasks.await

class WishlistRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addToWishlist(book: Book): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            db.collection("users").document(userId)
                .update("wishlist", FieldValue.arrayUnion(book.isbn))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFromWishlist(isbn: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            db.collection("users").document(userId)
                .update("wishlist", FieldValue.arrayRemove(isbn))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWishlist(): List<String> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val document = db.collection("users").document(userId).get().await()
            document.get("wishlist") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserWishlist(userId: String): List<String> {
        return try {
            val document = db.collection("users").document(userId).get().await()
            document.get("wishlist") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}