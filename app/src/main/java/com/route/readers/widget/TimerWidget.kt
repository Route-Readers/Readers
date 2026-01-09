package com.route.readers.widget

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
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_STOP_TIMER -> {
                // 타이머 초기화 및 페이지 기록 액티비티 실행
                resetTimer(context)
                openPageRecordActivity(context)
            }
            ACTION_PLAY_PAUSE -> {
                // 플레이/일시정지 토글
                toggleTimer(context)
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
                putLong("timer_start_time", System.currentTimeMillis())
            }
            apply()
        }
    }

    private fun openPageRecordActivity(context: Context) {
        val intent = Intent(context, UpdatePageCountActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}
