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

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // FCM 토큰을 Firestore에 저장
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (ReadersApplication.isAppInForeground) {
            // 데이터 메시지 처리
            if (message.data.isNotEmpty()) {
                val title = message.data["title"] ?: "새 알림"
                val body = message.data["message"] ?: "새로운 메시지가 도착했습니다."
                showNotification(title, body)
            } else {
                // 알림 메시지 처리 (기존 로직)
                message.notification?.let {
                    showNotification(it.title ?: "새 알림", it.body ?: "새로운 메시지가 도착했습니다.")
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
