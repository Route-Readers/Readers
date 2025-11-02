package com.route.readers.notification
// 현재 사용하지 않는 코드 파일이지만 추후 파이어베이스 업그레이드하면 FCM 기반으로 친구메시지 보낼sudo..
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FCMTokenManager {
    
    suspend fun updateFCMToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
                
            android.util.Log.d("FCMTokenManager", "FCM token updated: $token")
        } catch (e: Exception) {
            android.util.Log.e("FCMTokenManager", "Failed to update FCM token", e)
        }
    }
}
