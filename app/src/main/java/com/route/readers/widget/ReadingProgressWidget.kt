package com.route.readers.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.route.readers.MainActivity
import com.route.readers.R
import com.route.readers.data.remote.MyLibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadingProgressWidget : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.widget_reading_progress)
        
        // 앱 실행 인텐트
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // 데이터 로드 및 업데이트
        loadCurrentBook(context, views, appWidgetManager, appWidgetId)
    }

    private fun loadCurrentBook(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = MyLibraryRepository()
                repository.syncWithFirestore()
                val books = repository.myBooks.value

                val currentBook = books
                    .filter { it.progressPercentage < 100 }
                    .maxByOrNull { it.progressPercentage }

                val bitmap = currentBook?.let {
                    if (it.cover.isNotEmpty()) {
                        WidgetImageLoader.loadBitmap(context, it.getHighQualityImageUrl())
                    } else null
                }

                withContext(Dispatchers.Main) {
                    if (currentBook != null) {
                        views.setTextViewText(R.id.widget_book_title, currentBook.title)
                        views.setTextViewText(R.id.widget_book_author, currentBook.author)
                        views.setTextViewText(R.id.widget_progress, "${currentBook.progressPercentage}%")
                        views.setProgressBar(R.id.widget_progress_bar, 100, currentBook.progressPercentage, false)

                        val pageInfo = if (currentBook.totalPages > 0) {
                            "${currentBook.currentPage} / ${currentBook.totalPages} 페이지"
                        } else {
                            "페이지 정보 없음"
                        }
                        views.setTextViewText(R.id.widget_page_info, pageInfo)

                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_book_cover, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_book_cover, R.drawable.book_cover_placeholder)
                        }
                    } else {
                        views.setTextViewText(R.id.widget_book_title, "📖 읽고 있는 책이 없습니다")
                        views.setTextViewText(R.id.widget_book_author, "새로운 책을 추가해보세요!")
                        views.setTextViewText(R.id.widget_progress, "0%")
                        views.setTextViewText(R.id.widget_page_info, "")
                        views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                        views.setImageViewResource(R.id.widget_book_cover, R.drawable.book_cover_placeholder)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_book_title, "데이터 로드 실패")
                    views.setTextViewText(R.id.widget_book_author, "")
                    views.setTextViewText(R.id.widget_progress, "0%")
                    views.setTextViewText(R.id.widget_page_info, "")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
