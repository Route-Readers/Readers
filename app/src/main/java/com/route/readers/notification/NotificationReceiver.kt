package com.route.readers.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_DAILY_READING_REMINDER = "com.route.readers.DAILY_READING_REMINDER"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // 부팅 완료 시 알림 스케줄 재설정
                DailyNotificationScheduler.scheduleDailyNotification(context)
            }
            ACTION_DAILY_READING_REMINDER -> {
                // 매일 저녁 8시 알림 실행
                val notificationManager = ReadingNotificationManager(context)
                notificationManager.showReadingReminder()
                
                // 다음날 알림 스케줄
                DailyNotificationScheduler.scheduleDailyNotification(context)
            }
        }
    }
}
