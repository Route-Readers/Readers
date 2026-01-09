package com.route.readers

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class WidgetActionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activity layout is not needed as it's transparent and finishes immediately.

        intent?.let {
            val appWidgetId = it.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val action = it.getStringExtra(EXTRA_WIDGET_ACTION)

            Log.d("WidgetActionActivity", "Action: $action, WidgetId: $appWidgetId")

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && action != null) {
                val serviceIntent = Intent(this, TimerService::class.java).apply {
                    this.action = action // Use the action passed from widget
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(TimerService.EXTRA_STARTED_FROM_WIDGET, true)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        }
        finish() // Finish the activity immediately
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "com.route.readers.EXTRA_WIDGET_ACTION"
    }
}
