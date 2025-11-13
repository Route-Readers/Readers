package com.route.readers.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.route.readers.util.SharedPreferencesManager

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_READING_REMINDER = "com.route.readers.DAILY_READING_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // SharedPreferences에서 알림 설정 상태를 확인
        val notificationsEnabled = SharedPreferencesManager.isNotificationEnabled(context)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // 부팅 완료 시 알림이 활성화되어 있을 때만 스케줄 재설정
                if (notificationsEnabled) {
                    DailyNotificationScheduler.scheduleDailyNotification(context)
                }
            }
            ACTION_DAILY_READING_REMINDER -> {
                // 매일 알림 실행
                val notificationManager = ReadingNotificationManager(context)
                notificationManager.showReadingReminder()

                // 다음날 알림 스케줄 (알림이 활성화되어 있을 때만)
                if (notificationsEnabled) {
                    DailyNotificationScheduler.scheduleDailyNotification(context)
                }
            }
        }
    }
}
