package com.route.readers.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private fun getUsersCollection() = db.collection("users")

    suspend fun getUser(userId: String): User? {
        return try {
            getUsersCollection().document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
