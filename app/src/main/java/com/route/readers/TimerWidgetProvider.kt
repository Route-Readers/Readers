package com.route.readers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.route.readers.TimerService

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TIMER = "com.route.readers.ACTION_TOGGLE_TIMER"
        const val ACTION_RESET_TIMER = "com.route.readers.ACTION_RESET_TIMER" // New action
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.timer_widget_layout
            ).apply {
                // Initial text set by TimerService eventually
                setTextViewText(R.id.timer_text_view, "00:00:00") // Set default

                // PendingIntent for Play/Pause button
                val toggleTimerIntent = Intent(context, TimerService::class.java).apply {
                    action = ACTION_TOGGLE_TIMER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val togglePendingIntent: PendingIntent = PendingIntent.getService(
                    context,
                    appWidgetId, // Use appWidgetId as request code for uniqueness
                    toggleTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.play_pause_button, togglePendingIntent)

                // PendingIntent for Reset button
                val resetTimerIntent = Intent(context, TimerService::class.java).apply {
                    action = ACTION_RESET_TIMER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val resetPendingIntent: PendingIntent = PendingIntent.getService(
                    context,
                    appWidgetId + 1, // Use a different request code for reset
                    resetTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.reset_button, resetPendingIntent)

                // PendingIntent for Refresh button
                val refreshBookDataIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_UPDATE_BOOK_DATA
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val refreshPendingIntent: PendingIntent = PendingIntent.getService(
                    context,
                    appWidgetId + 2, // Use a different request code for refresh
                    refreshBookDataIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Explicitly trigger book data update for this widget instance
            val updateBookDataIntent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_UPDATE_BOOK_DATA
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            context.startService(updateBookDataIntent)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Stop the timer service if it's running
        val stopServiceIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_TIMER
        }
        context.stopService(stopServiceIntent)
    }
}
