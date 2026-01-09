package com.route.readers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TOGGLE_TIMER = "com.route.readers.WIDGET_TOGGLE_TIMER"
        const val ACTION_WIDGET_STOP_TIMER = "com.route.readers.WIDGET_STOP_TIMER"
        const val ACTION_WIDGET_SELECT_BOOK = "com.route.readers.WIDGET_SELECT_BOOK"

        const val EXTRA_BOOK_ISBN = "com.route.readers.EXTRA_BOOK_ISBN"
        const val EXTRA_CURRENT_PAGE = "com.route.readers.EXTRA_CURRENT_PAGE"

        const val WIDGET_PREFS_NAME = "widget_preferences"
        const val CURRENT_BOOK_ISBN_PREF = "current_reading_book_isbn_for_widget"
        const val CURRENT_BOOK_PAGE_PREF = "current_reading_book_page_for_widget"
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
                setTextViewText(R.id.timer_text_view, "00:00:00")

                // Play/Pause button
                val toggleTimerIntent = Intent(context, WidgetActionActivity::class.java).apply {
                    putExtra(WidgetActionActivity.EXTRA_WIDGET_ACTION, TimerService.ACTION_TOGGLE_TIMER)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val togglePendingIntent: PendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10 + 1,
                    toggleTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                setOnClickPendingIntent(R.id.play_pause_button, togglePendingIntent)

                // Stop button - Direct Broadcast
                val stopTimerIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_STOP_TIMER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val stopTimerPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 2,
                    stopTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.stop_button, stopTimerPendingIntent)

                // Book selection
                val bookSelectionIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_SELECT_BOOK
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val bookSelectionPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 3,
                    bookSelectionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.book_info_container, bookSelectionPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)

            val updateBookDataIntent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_UPDATE_BOOK_DATA
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            ContextCompat.startForegroundService(context, updateBookDataIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        
        when (intent.action) {
            ACTION_WIDGET_STOP_TIMER -> {
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_STOP_TIMER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            ACTION_WIDGET_SELECT_BOOK -> {
                val activityIntent = Intent(context, BookSelectionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                context.startActivity(activityIntent)
            }
        }
    }

    override fun onDisabled(context: Context) {
        val stopServiceIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_TIMER
        }
        context.stopService(stopServiceIntent)
    }
}
