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

        val notificationTitle = message.notification?.title ?: message.data["title"] ?: "새 알림"
        val notificationBody = message.notification?.body ?: message.data["message"] ?: "새로운 메시지가 도착했습니다."
        val notificationData = message.data

        android.util.Log.d("FCM_SERVICE", "onMessageReceived: Processing FCM message.")
        showNotification(notificationTitle, notificationBody, notificationData)
    }

    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        android.util.Log.d("FCM_SERVICE", "showNotification called. Title='$title', Message='$message', Data='$data'")
        val channelId = "reading_notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        android.util.Log.d("FCM_SERVICE", "showNotification: NotificationManager obtained.")

        val intent = Intent(this, MainActivity::class.java)
        // Add data to the intent
        for ((key, value) in data) {
            intent.putExtra(key, value)
        }
        // Set flags to clear activity stack and create a new task
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        android.util.Log.d("FCM_SERVICE", "showNotification: Intent created with data.")

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Use unique request code for each notification
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        android.util.Log.d("FCM_SERVICE", "showNotification: PendingIntent created.")

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        android.util.Log.d("FCM_SERVICE", "showNotification: Notification built.")

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        android.util.Log.d("FCM_SERVICE", "Notification issued with ID: $notificationId")
    }
}