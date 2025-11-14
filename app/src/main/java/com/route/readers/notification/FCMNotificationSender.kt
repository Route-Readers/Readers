package com.route.readers.notification

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FCMNotificationSender {

    private const val TAG = "FCMNotificationSender"

    suspend fun sendNotificationRequest(
        targetUserId: String,
        senderId: String,
        senderNickname: String,
        notificationType: String,
        title: String,
        body: String
    ) {
        try {
            val db = FirebaseFirestore.getInstance()
            val fcmRequest = hashMapOf(
                "targetUserId" to targetUserId,
                "senderId" to senderId,
                "senderNickname" to senderNickname,
                "notificationType" to notificationType,
                "title" to title,
                "message" to body,
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("fcmRequests").add(fcmRequest).await()
            Log.d(TAG, "FCM notification request sent for type: $notificationType to $targetUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM notification request: ${e.message}", e)
        }
    }
}
