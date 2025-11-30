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
        const val ACTION_TOGGLE_TIMER = "com.route.readers.ACTION_TOGGLE_TIMER"
        const val ACTION_RESET_TIMER = "com.route.readers.ACTION_RESET_TIMER"
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

                // PendingIntent for Stop button: stop timer service first, then open page update flow
                val stopTimerIntent = Intent(context, TimerService::class.java).apply {
                    action = ACTION_STOP_TIMER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val stopTimerPendingIntent: PendingIntent = PendingIntent.getService(
                    context,
                    appWidgetId + 2,
                    stopTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.stop_button, stopTimerPendingIntent)

                // PendingIntent for clicking the book info area
                val bookSelectionIntent = Intent(context, BookSelectionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Needed to launch Activity from non-Activity context
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) // Pass widget ID back
                }
                val bookSelectionPendingIntent: PendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 3, // Use a different request code for book selection
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
