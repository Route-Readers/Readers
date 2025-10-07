package com.route.readers.notification

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class ReadingNotificationService : Service() {
    
    private lateinit var notificationManager: ReadingNotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var notificationRunnable: Runnable? = null
    
    companion object {
        const val NOTIFICATION_INTERVAL = 10_000L // 10초
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = ReadingNotificationManager(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startNotificationScheduler()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startNotificationScheduler() {
        notificationRunnable = object : Runnable {
            override fun run() {
                notificationManager.showReadingReminder()
                handler.postDelayed(this, NOTIFICATION_INTERVAL)
            }
        }
        handler.post(notificationRunnable!!)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        notificationRunnable?.let { handler.removeCallbacks(it) }
    }
}
