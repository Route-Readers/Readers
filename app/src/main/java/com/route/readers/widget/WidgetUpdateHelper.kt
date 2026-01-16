package com.route.readers.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.route.readers.TimerService
import com.route.readers.TimerWidgetProvider

object WidgetUpdateHelper {
    
    private var appContext: Context? = null
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    fun updateAllWidgets() {
        appContext?.let { context ->
            val intent = Intent(context, ReadingProgressWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, ReadingProgressWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
            
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            context.sendBroadcast(intent)

            // Timer widget도 현재 책 정보/표시를 최신화
            val timerComponent = ComponentName(context, TimerWidgetProvider::class.java)
            val timerWidgetIds = appWidgetManager.getAppWidgetIds(timerComponent)
            val timerUpdateIntent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_UPDATE_BOOK_DATA
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, timerWidgetIds)
                putExtra(TimerService.EXTRA_STOP_AFTER_UPDATE, true) // Stop service after update if it was just for this
            }
            
            try {
                // Try starting as a normal service first (if app is in foreground)
                context.startService(timerUpdateIntent)
            } catch (e: Exception) {
                // If it fails (background restriction), start as foreground service
                ContextCompat.startForegroundService(context, timerUpdateIntent)
            }
        }
    }
}
