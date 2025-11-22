package com.route.readers

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import java.util.Locale

class TimerService : Service() {

    private var timerRunning = false
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private val updateTimerTask = object : Runnable {
        override fun run() {
            if (timerRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                updateWidget()
                handler.postDelayed(this, 1000) // Update every second
            }
        }
    }

    companion object {
        const val ACTION_START_TIMER = "com.route.readers.ACTION_START_TIMER"
        const val ACTION_STOP_TIMER = "com.route.readers.ACTION_STOP_TIMER"
        const val ACTION_TOGGLE_TIMER = "com.route.readers.ACTION_TOGGLE_TIMER"
        const val ACTION_RESET_TIMER = "com.route.readers.ACTION_RESET_TIMER" // New action
        private const val TAG = "TimerService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            appWidgetId = it.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            when (it.action) {
                ACTION_TOGGLE_TIMER -> {
                    if (timerRunning) {
                        stopTimer()
                    } else {
                        startTimer()
                    }
                }
                ACTION_START_TIMER -> startTimer()
                ACTION_STOP_TIMER -> stopTimer()
                ACTION_RESET_TIMER -> resetTimer() // Handle new reset action
            }
        }
        return START_STICKY // Service will be restarted if killed
    }

    private fun startTimer() {
        if (!timerRunning) {
            timerRunning = true
            startTime = System.currentTimeMillis() - elapsedTime // Resume from last elapsed time
            handler.post(updateTimerTask)
            Log.d(TAG, "Timer started. appWidgetId: $appWidgetId")
            updateWidgetButton(true) // Update button to pause
        }
    }

    private fun stopTimer() {
        if (timerRunning) {
            timerRunning = false
            handler.removeCallbacks(updateTimerTask)
            Log.d(TAG, "Timer stopped. appWidgetId: $appWidgetId")
            updateWidgetButton(false) // Update button to play
        }
    }

    private fun resetTimer() {
        stopTimer() // Stop the timer if it's running
        elapsedTime = 0L // Reset elapsed time
        updateWidget() // Update widget to show "00:00:00"
        updateWidgetButton(false) // Ensure play button is shown
        Log.d(TAG, "Timer reset. appWidgetId: $appWidgetId")
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val remoteViews = RemoteViews(packageName, R.layout.timer_widget_layout)

        val hours = (elapsedTime / 3600000).toInt()
        val minutes = (elapsedTime % 3600000 / 60000).toInt()
        val seconds = (elapsedTime % 60000 / 1000).toInt()

        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        remoteViews.setTextViewText(R.id.timer_text_view, timeFormatted)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        } else {
            // If appWidgetId is invalid, try to update all widgets of this type
            val componentName = ComponentName(this, TimerWidgetProvider::class.java)
            appWidgetManager.updateAppWidget(componentName, remoteViews)
        }
        Log.d(TAG, "Widget updated: $timeFormatted")
    }

    private fun updateWidgetButton(isPlaying: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val remoteViews = RemoteViews(packageName, R.layout.timer_widget_layout)

        val iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        remoteViews.setImageViewResource(R.id.play_pause_button, iconRes)

        // Set up the PendingIntent again for the button click (play/pause)
        val toggleTimerIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TOGGLE_TIMER
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val togglePendingIntent: PendingIntent = PendingIntent.getService(
            this,
            appWidgetId, // Use appWidgetId as request code for uniqueness
            toggleTimerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.play_pause_button, togglePendingIntent)

        // Set up PendingIntent for the reset button
        val resetTimerIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_RESET_TIMER
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val resetPendingIntent: PendingIntent = PendingIntent.getService(
            this,
            appWidgetId + 1, // Use a different request code for reset
            resetTimerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.reset_button, resetPendingIntent)


        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        } else {
            val componentName = ComponentName(this, TimerWidgetProvider::class.java)
            appWidgetManager.updateAppWidget(componentName, remoteViews)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null // This service does not allow binding
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer() // Ensure timer is stopped when service is destroyed
        Log.d(TAG, "TimerService destroyed.")
    }
}
