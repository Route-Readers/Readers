package com.route.readers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.route.readers.TimerService
import com.route.readers.TimerService.Companion.ACTION_STOP_TIMER

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        // Actions for BroadcastReceiver in this provider
        const val ACTION_WIDGET_TOGGLE_TIMER = "com.route.readers.WIDGET_TOGGLE_TIMER"
        const val ACTION_WIDGET_STOP_TIMER = "com.route.readers.WIDGET_STOP_TIMER"
        const val ACTION_WIDGET_SELECT_BOOK = "com.route.readers.WIDGET_SELECT_BOOK"

        // Intent Extras
        const val EXTRA_BOOK_ISBN = "com.route.readers.EXTRA_BOOK_ISBN"
        const val EXTRA_CURRENT_PAGE = "com.route.readers.EXTRA_CURRENT_PAGE"

        // SharedPreferences keys - must match those used in TimerService
        const val WIDGET_PREFS_NAME = "widget_preferences"
        const val CURRENT_BOOK_ISBN_PREF = "current_reading_book_isbn_for_widget"
        const val CURRENT_BOOK_PAGE_PREF = "current_reading_book_page_for_widget"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val sharedPrefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        val bookIsbn = sharedPrefs.getString(CURRENT_BOOK_ISBN_PREF, null)
        val currentPage = sharedPrefs.getInt(CURRENT_BOOK_PAGE_PREF, 0)

        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.timer_widget_layout
            ).apply {
                // Initial text set by TimerService eventually
                setTextViewText(R.id.timer_text_view, "00:00:00") // Set default

                // PendingIntent for Play/Pause button (using Broadcast)
                val toggleTimerIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_TOGGLE_TIMER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val togglePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId, // Use appWidgetId as request code for uniqueness
                    toggleTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.play_pause_button, togglePendingIntent)

                // PendingIntent for Stop button (using Broadcast)
                val stopTimerIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_STOP_TIMER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val stopTimerPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 2, // Use a different request code
                    stopTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.stop_button, stopTimerPendingIntent)

                // PendingIntent for clicking the book info area (using Broadcast)
                val bookSelectionIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_SELECT_BOOK
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val bookSelectionPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 3, // Use a different request code
                    bookSelectionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.book_info_container, bookSelectionPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Explicitly trigger book data update for this widget instance
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
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && intent.action !in listOf(AppWidgetManager.ACTION_APPWIDGET_DELETED, AppWidgetManager.ACTION_APPWIDGET_DISABLED)) {
            // We need a widget ID for most actions. If it's not here, and it's not a general non-ID action, bail.
            return
        }

        when (intent.action) {
            ACTION_WIDGET_TOGGLE_TIMER, ACTION_WIDGET_STOP_TIMER -> {
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    // Translate widget action to service action
                    action = if (intent.action == ACTION_WIDGET_TOGGLE_TIMER) {
                        TimerService.ACTION_TOGGLE_TIMER
                    } else {
                        TimerService.ACTION_STOP_TIMER
                    }
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(TimerService.EXTRA_STARTED_FROM_WIDGET, true) // Add this extra
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
