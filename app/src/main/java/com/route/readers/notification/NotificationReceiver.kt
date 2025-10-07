package com.route.readers.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // 부팅 완료 시 알림 서비스 시작
                val serviceIntent = Intent(context, ReadingNotificationService::class.java)
                context.startService(serviceIntent)
            }
        }
    }
}
