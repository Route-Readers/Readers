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
                    .filter { it.progress < 100 }
                    .maxByOrNull { it.progress }
                
                CoroutineScope(Dispatchers.Main).launch {
                    if (currentBook != null) {
                        views.setTextViewText(R.id.widget_book_title, currentBook.title)
                        views.setTextViewText(R.id.widget_book_author, currentBook.author)
                        views.setTextViewText(R.id.widget_progress, "${currentBook.progress}%")
                        views.setProgressBar(R.id.widget_progress_bar, 100, currentBook.progress, false)
                        
                        // 페이지 정보 표시
                        val pageInfo = if (currentBook.totalPages > 0) {
                            "${currentBook.currentPage} / ${currentBook.totalPages} 페이지"
                        } else {
                            "페이지 정보 없음"
                        }
                        views.setTextViewText(R.id.widget_page_info, pageInfo)
                        
                        // 책 표지 이미지 로드
                        if (currentBook.cover.isNotEmpty()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val bitmap = WidgetImageLoader.loadBitmap(context, currentBook.cover)
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (bitmap != null) {
                                        views.setImageViewBitmap(R.id.widget_book_cover, bitmap)
                                    }
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                            }
                        } else {
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    } else {
                        views.setTextViewText(R.id.widget_book_title, "📖 읽고 있는 책이 없습니다")
                        views.setTextViewText(R.id.widget_book_author, "새로운 책을 추가해보세요!")
                        views.setTextViewText(R.id.widget_progress, "0%")
                        views.setTextViewText(R.id.widget_page_info, "")
                        views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
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
