package com.route.readers.notification

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

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        val channelId = "reading_notifications"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "독서 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("FCM_SERVICE", "Notification Channel '$channelId' created.")
        } else {
            android.util.Log.d("FCM_SERVICE", "Notification Channels not supported below Android O.")
        }
    }

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

        message.notification?.let {
            android.util.Log.d("FCM_SERVICE", "onMessageReceived: message.notification is present. Calling showNotification.")
            showNotification(it.title ?: "새 알림", it.body ?: "새로운 메시지가 도착했습니다.")
        } ?: run {
            android.util.Log.d("FCM_SERVICE", "onMessageReceived: message.notification is NULL. Not calling showNotification.")
        }
    }

    private fun showNotification(title: String, message: String) {
        android.util.Log.d("FCM_SERVICE", "Showing notification: Title='$title', Message='$message'")

        val channelId = "reading_notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

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
        android.util.Log.d("FCM_SERVICE", "Notification issued with ID: ${System.currentTimeMillis().toInt()}")
    }
}