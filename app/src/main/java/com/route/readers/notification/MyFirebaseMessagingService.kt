package com.route.readers.notification
// 얘도 현재 사용하지 않지만 나중에 필요할수도 ..
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.route.readers.MainActivity
import com.route.readers.R
import com.route.readers.ReadersApplication // Add this import

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(com.route.readers.data.model.User::class.java)
                    val followAlarmEnabled = user?.followAlarmEnabled ?: true // Default to true if not set

                    if (ReadersApplication.isAppInForeground) {
                        // 데이터 메시지 처리
                        if (message.data.isNotEmpty()) {
                            val type = message.data["type"]
                            val title = message.data["title"] ?: "새 알림"
                            val body = message.data["message"] ?: "새로운 메시지가 도착했습니다."

                            if (type == "follow" || title == "새로운 팔로워") {
                                if (followAlarmEnabled) {
                                    showNotification(title, body)
                                }
                            } else {
                                // Other data messages always shown
                                showNotification(title, body)
                            }
                        } else {
                            // 알림 메시지 처리 (기존 로직) - Firebase Console에서 보내는 알림
                            message.notification?.let {
                                val title = it.title ?: "새 알림"
                                val body = it.body ?: "새로운 메시지가 도착했습니다."

                                // Heuristic to guess if it's a follow notification from title
                                if (title == "새로운 팔로워") {
                                    if (followAlarmEnabled) {
                                        showNotification(title, body)
                                    }
                                } else {
                                    showNotification(title, body)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle error fetching user settings
                    // For now, log and proceed with showing notification to avoid missing critical alerts
                    // In a production app, more robust error handling and fallback might be needed
                    if (ReadersApplication.isAppInForeground) {
                        if (message.data.isNotEmpty()) {
                            val title = message.data["title"] ?: "새 알림"
                            val body = message.data["message"] ?: "새로운 메시지가 도착했습니다."
                            showNotification(title, body)
                        } else {
                            message.notification?.let {
                                showNotification(it.title ?: "새 알림", it.body ?: "새로운 메시지가 도착했습니다.")
                            }
                        }
                    }
                }
        } else {
            // If user is not logged in, proceed with original behavior (show all notifications)
            if (ReadersApplication.isAppInForeground) {
                if (message.data.isNotEmpty()) {
                    val title = message.data["title"] ?: "새 알림"
                    val body = message.data["message"] ?: "새로운 메시지가 도착했습니다."
                    showNotification(title, body)
                } else {
                    message.notification?.let {
                        showNotification(it.title ?: "새 알림", it.body ?: "새로운 메시지가 도착했습니다.")
                    }
                }
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "reading_notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "독서 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
