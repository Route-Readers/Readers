package com.route.readers.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FCMTokenManager {

    private const val TAG = "FCMTokenManager"

    suspend fun updateFCMToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found. Cannot update FCM token.")
            return
        }

        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val userId = currentUser.uid

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()

            Log.d(TAG, "FCM token updated successfully for user $userId: $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token: ${e.message}", e)
        }
    }
}