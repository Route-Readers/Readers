package com.route.readers.service

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.route.readers.R
import com.route.readers.widget.ReadingProgressWidget
import java.util.concurrent.TimeUnit

class ReadingTimerService : Service() {

    private var isTimerRunning: Boolean = false
    private var currentBookId: String? = null
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L // Total time read for the current session + previously saved time

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning && currentBookId != null) {
                val currentSessionTime = System.currentTimeMillis() - startTime
                val totalElapsedTime = elapsedTime + currentSessionTime
                updateWidgetTimerDisplay(totalElapsedTime)
                handler.postDelayed(this, 1000) // Update every second
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val bookId = intent?.getStringExtra(EXTRA_BOOK_ID)

        if (bookId != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            currentBookId = bookId
            if (action == ACTION_TOGGLE_TIMER) {
                toggleTimer(appWidgetId)
            }
        } else {
            Log.w(TAG, "Invalid bookId or appWidgetId received.")
        }
        return START_STICKY
    }

    private fun toggleTimer(appWidgetId: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedElapsedTime = sharedPrefs.getLong("$currentBookId-elapsedTime", 0L)
        val editor = sharedPrefs.edit()

        if (isTimerRunning) {
            // Pause timer
            val currentSessionTime = System.currentTimeMillis() - startTime
            elapsedTime += currentSessionTime
            editor.putLong("$currentBookId-elapsedTime", elapsedTime)
            editor.putBoolean("$currentBookId-isTimerRunning", false) // Save state
            editor.apply()

            handler.removeCallbacks(updateRunnable)
            isTimerRunning = false
            Log.d(TAG, "Timer Paused for book $currentBookId. Total elapsed: ${formatTime(elapsedTime)}")
        } else {
            // Start timer
            elapsedTime = savedElapsedTime // Load previously saved time
            startTime = System.currentTimeMillis()
            editor.putBoolean("$currentBookId-isTimerRunning", true) // Save state
            editor.apply()

            handler.post(updateRunnable)
            isTimerRunning = true
            Log.d(TAG, "Timer Started for book $currentBookId. Starting from: ${formatTime(elapsedTime)}")
        }
        updateWidget(appWidgetId)
    }

    private fun updateWidgetTimerDisplay(timeInMillis: Long) {
        val formattedTime = formatTime(timeInMillis)
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, ReadingProgressWidget::class.java)

        // Update main widget layout
        val remoteViews = RemoteViews(packageName, R.layout.widget_reading_progress)
        remoteViews.setTextViewText(R.id.widget_reading_timer, formattedTime)
        remoteViews.setViewVisibility(R.id.widget_reading_timer, View.VISIBLE)
        appWidgetManager.updateAppWidget(componentName, remoteViews)

        // Update minimal widget layout
        val minimalRemoteViews = RemoteViews(packageName, R.layout.widget_reading_progress_minimal)
        minimalRemoteViews.setTextViewText(R.id.widget_reading_timer, formattedTime)
        minimalRemoteViews.setViewVisibility(R.id.widget_reading_timer, View.VISIBLE)
        appWidgetManager.updateAppWidget(componentName, minimalRemoteViews)
    }

    private fun updateWidget(appWidgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, ReadingProgressWidget::class.java)

        // Update main widget layout
        val remoteViews = RemoteViews(packageName, R.layout.widget_reading_progress)
        val playPauseIcon = if (isTimerRunning) R.drawable.ic_pause else R.drawable.ic_play_arrow
        remoteViews.setImageViewResource(R.id.widget_play_button, playPauseIcon)
        // Set initial timer display (or load saved)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedElapsedTime = sharedPrefs.getLong("$currentBookId-elapsedTime", 0L)
        remoteViews.setTextViewText(R.id.widget_reading_timer, formatTime(savedElapsedTime))
        remoteViews.setViewVisibility(R.id.widget_reading_timer, View.VISIBLE) // Make timer visible
        appWidgetManager.updateAppWidget(componentName, remoteViews)


        // Update minimal widget layout if it exists and has a play button
        val minimalRemoteViews = RemoteViews(packageName, R.layout.widget_reading_progress_minimal)
        minimalRemoteViews.setImageViewResource(R.id.widget_play_button, playPauseIcon)
        minimalRemoteViews.setTextViewText(R.id.widget_reading_timer, formatTime(savedElapsedTime))
        minimalRemoteViews.setViewVisibility(R.id.widget_reading_timer, View.VISIBLE) // Make timer visible
        appWidgetManager.updateAppWidget(componentName, minimalRemoteViews)
    }

    private fun formatTime(timeInMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        if (isTimerRunning && currentBookId != null) {
            val currentSessionTime = System.currentTimeMillis() - startTime
            elapsedTime += currentSessionTime
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong("$currentBookId-elapsedTime", elapsedTime)
                .putBoolean("$currentBookId-isTimerRunning", false) // Timer is no longer running on destroy
                .apply()
            Log.d(TAG, "Reading Timer Service Destroyed. Final elapsed for $currentBookId: ${formatTime(elapsedTime)}")
        }
        Log.d(TAG, "Reading Timer Service Destroyed")
    }

    companion object {
        const val ACTION_TOGGLE_TIMER = "com.route.readers.action.TOGGLE_TIMER"
        const val EXTRA_BOOK_ID = "com.route.readers.extra.BOOK_ID"
        val PREFS_NAME = "ReadingTimerPrefs" // Changed to public/internal
        private const val TAG = "ReadingTimerService"
    }
}