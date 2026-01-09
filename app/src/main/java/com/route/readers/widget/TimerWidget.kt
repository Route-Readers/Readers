package com.route.readers.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.route.readers.R
import com.route.readers.UpdatePageCountActivity

class TimerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_STOP_TIMER = "com.route.readers.widget.STOP_TIMER"
        const val ACTION_PLAY_PAUSE = "com.route.readers.widget.PLAY_PAUSE"
        const val ACTION_UPDATE_TIMER = "com.route.readers.widget.UPDATE_TIMER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_STOP_TIMER -> {
                resetTimer(context)
                stopTimerUpdates(context)
                updateAllWidgets(context)
                openPageRecordActivity(context)
            }
            ACTION_PLAY_PAUSE -> {
                toggleTimer(context)
                val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
                val isRunning = prefs.getBoolean("timer_running", false)
                if (isRunning) {
                    startTimerUpdates(context)
                } else {
                    stopTimerUpdates(context)
                }
                updateAllWidgets(context)
            }
            ACTION_UPDATE_TIMER -> {
                updateAllWidgets(context)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.timer_widget_layout)
        
        // 타이머 시간 업데이트
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("timer_running", false)
        val startTime = prefs.getLong("timer_start_time", 0)
        val elapsedTime = prefs.getLong("elapsed_time", 0)
        
        val totalElapsed = if (isRunning && startTime > 0) {
            elapsedTime + (System.currentTimeMillis() - startTime)
        } else {
            elapsedTime
        }
        
        val timeString = formatTime(totalElapsed)
        views.setTextViewText(R.id.timer_text_view, timeString)
        
        // 플레이/일시정지 버튼 아이콘 업데이트
        val playPauseIcon = if (isRunning) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        views.setImageViewResource(R.id.play_pause_button, playPauseIcon)
        
        // 정지 버튼 클릭 인텐트
        val stopIntent = Intent(context, TimerWidget::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.stop_button, stopPendingIntent)
        
        // 플레이/일시정지 버튼 클릭 인텐트
        val playPauseIntent = Intent(context, TimerWidget::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.play_pause_button, playPausePendingIntent)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun resetTimer(context: Context) {
        // SharedPreferences에서 타이머 상태 초기화
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("timer_start_time", 0)
            putBoolean("timer_running", false)
            putLong("elapsed_time", 0)
            apply()
        }
    }

    private fun toggleTimer(context: Context) {
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("timer_running", false)
        
        prefs.edit().apply {
            putBoolean("timer_running", !isRunning)
            if (!isRunning) {
                // 타이머 재시작 시 현재 시간을 시작 시간으로 설정
                putLong("timer_start_time", System.currentTimeMillis())
            } else {
                // 타이머 일시정지 시 누적 시간 저장
                val startTime = prefs.getLong("timer_start_time", 0)
                val currentElapsed = prefs.getLong("elapsed_time", 0)
                val sessionTime = System.currentTimeMillis() - startTime
                putLong("elapsed_time", currentElapsed + sessionTime)
            }
            apply()
        }
    }

    private fun openPageRecordActivity(context: Context) {
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val elapsedTime = prefs.getLong("elapsed_time", 0)
        
        // 독서 시간이 있을 때만 페이지 업데이트 화면 열기
        if (elapsedTime > 0) {
            val intent = Intent(context, UpdatePageCountActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // 필요한 경우 현재 읽는 책 정보 전달
                putExtra("reading_time", elapsedTime)
            }
            context.startActivity(intent)
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, TimerWidget::class.java)
        )
        onUpdate(context, appWidgetManager, widgetIds)
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun startTimerUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerWidget::class.java).apply {
            action = ACTION_UPDATE_TIMER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + 1000,
            1000,
            pendingIntent
        )
    }

    private fun stopTimerUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerWidget::class.java).apply {
            action = ACTION_UPDATE_TIMER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
