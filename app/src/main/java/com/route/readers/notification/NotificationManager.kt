package com.route.readers.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.route.readers.MainActivity
import com.route.readers.R
import kotlin.random.Random

class ReadingNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "reading_reminders"
        const val CHANNEL_NAME = "독서 알림"
        const val CHANNEL_DESCRIPTION = "독서 습관 형성을 위한 알림"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // HIGH로 변경
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true) // 진동 활성화
                enableLights(true) // LED 활성화
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showReadingReminder() {
        val messages = listOf(
            "돌아와서 책을 함께 읽어봐요! 📚",
            "하루라도 책을 읽지 않으면 입안에 가시가 돋칠거에요ㅜㅜ 🌹",
            "친구들이 벌써 10페이지나 읽었어요! 📖",
            "오늘의 독서 목표를 달성해보세요! ⭐",
            "책 속에서 새로운 세상을 만나보세요! 🌟",
            "독서는 마음의 양식이에요! 🍯",
            "잠깐! 책이 당신을 기다리고 있어요! 📚✨",
            "독서 스트릭을 이어가세요! 🔥",
            "친구들과 함께 독서 챌린지에 참여해보세요! 🏆",
            "오늘 읽은 페이지를 기록해보세요! 📝"
        )
        
        val randomMessage = messages[Random.nextInt(messages.size)]
        val notificationId = Random.nextInt(1000)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // 기본 아이콘 사용
                .setContentTitle("Readers 📚")
                .setContentText(randomMessage)
                .setStyle(NotificationCompat.BigTextStyle().bigText(randomMessage))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // HIGH로 변경
                .setDefaults(NotificationCompat.DEFAULT_ALL) // 기본 사운드, 진동 등
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(notificationId, notification)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
