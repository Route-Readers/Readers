package com.route.readers.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdateHelper {
    
    private var appContext: Context? = null
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    fun updateAllWidgets() {
        appContext?.let { context ->
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // Update ReadingProgressWidget
            val widgetComponent = ComponentName(context, ReadingProgressWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, ReadingProgressWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }

            // Update ReadingProgressWidgetSmall
            val widgetComponentSmall = ComponentName(context, ReadingProgressWidgetSmall::class.java)
            val widgetIdsSmall = appWidgetManager.getAppWidgetIds(widgetComponentSmall)
            if (widgetIdsSmall.isNotEmpty()) {
                val intentSmall = Intent(context, ReadingProgressWidgetSmall::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIdsSmall)
                }
                context.sendBroadcast(intentSmall)
            }

            // Update ReadingProgressWidgetLarge
            val widgetComponentLarge = ComponentName(context, ReadingProgressWidgetLarge::class.java)
            val widgetIdsLarge = appWidgetManager.getAppWidgetIds(widgetComponentLarge)
            if (widgetIdsLarge.isNotEmpty()) {
                val intentLarge = Intent(context, ReadingProgressWidgetLarge::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIdsLarge)
                }
                context.sendBroadcast(intentLarge)
            }

            // Update ReadingProgressWidget4x1
            val widgetComponent4x1 = ComponentName(context, ReadingProgressWidget4x1::class.java)
            val widgetIds4x1 = appWidgetManager.getAppWidgetIds(widgetComponent4x1)
            if (widgetIds4x1.isNotEmpty()) {
                val intent4x1 = Intent(context, ReadingProgressWidget4x1::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds4x1)
                }
                context.sendBroadcast(intent4x1)
            }

            // Update ReadingProgressWidget4x2
            val widgetComponent4x2 = ComponentName(context, ReadingProgressWidget4x2::class.java)
            val widgetIds4x2 = appWidgetManager.getAppWidgetIds(widgetComponent4x2)
            if (widgetIds4x2.isNotEmpty()) {
                val intent4x2 = Intent(context, ReadingProgressWidget4x2::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds4x2)
                }
                context.sendBroadcast(intent4x2)
            }
        }
    }
}
