package com.route.readers.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.route.readers.MainActivity
import com.route.readers.R
import com.route.readers.data.WidgetSettingsRepository
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.ui.screens.profile.WidgetColorScheme
import com.route.readers.ui.screens.profile.WidgetStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadingProgressWidget4x1 : AppWidgetProvider() {

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
        val settingsRepository = WidgetSettingsRepository(context)
        val style = settingsRepository.getWidgetStyle()
        val showFriendReading = settingsRepository.getShowFriendReading()
        val showProgressBar = settingsRepository.getShowProgressBar()
        val showPlayButton = settingsRepository.getShowPlayButton()
        val colorScheme = settingsRepository.getColorScheme()

        val layoutId = when (style) {
            WidgetStyle.NORMAL -> R.layout.widget_reading_progress
            WidgetStyle.MINIMAL -> R.layout.widget_reading_progress_minimal
        }
        val views = RemoteViews(context.packageName, layoutId)

        // 앱 실행 인텐트
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // 데이터 로드 및 업데이트
        loadCurrentBook(context, views, appWidgetManager, appWidgetId, showFriendReading, showProgressBar, showPlayButton, colorScheme)
    }

    private fun loadCurrentBook(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        showFriendReading: Boolean,
        showProgressBar: Boolean,
        showPlayButton: Boolean,
        colorScheme: WidgetColorScheme
    ) {
        val bgColor = when (colorScheme) {
            WidgetColorScheme.LIGHT -> R.color.widget_background_light
            WidgetColorScheme.DARK -> R.color.widget_background_dark
            else -> R.color.widget_background_system
        }
        views.setInt(R.id.widget_container, "setBackgroundResource", bgColor)

        val textColor = if (colorScheme == WidgetColorScheme.DARK) {
            android.R.color.white
        } else {
            android.R.color.black
        }
        views.setTextColor(R.id.widget_book_title, context.getColor(textColor))
        if (views.layoutId == R.layout.widget_reading_progress) {
            views.setTextColor(R.id.widget_book_author, context.getColor(textColor))
            views.setTextColor(R.id.widget_page_info, context.getColor(textColor))
        }
        views.setTextColor(R.id.widget_progress, context.getColor(textColor))
        if (views.layoutId == R.layout.widget_reading_progress_minimal) {
            views.setTextColor(R.id.widget_friend_reading, context.getColor(textColor))
        }


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
                        if (views.layoutId == R.layout.widget_reading_progress) {
                            views.setTextViewText(R.id.widget_book_author, currentBook.author)
                            val pageInfo = if (currentBook.totalPages > 0) {
                                "${currentBook.currentPage} / ${currentBook.totalPages} 페이지"
                            } else {
                                "페이지 정보 없음"
                            }
                            views.setTextViewText(R.id.widget_page_info, pageInfo)
                        }

                        if (showProgressBar) {
                            views.setViewVisibility(R.id.widget_progress_bar, View.VISIBLE)
                            views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
                            views.setTextViewText(R.id.widget_progress, "${currentBook.progressPercentage}%")
                            views.setProgressBar(R.id.widget_progress_bar, 100, currentBook.progressPercentage, false)
                        } else {
                            views.setViewVisibility(R.id.widget_progress_bar, View.GONE)
                            views.setViewVisibility(R.id.widget_progress, View.GONE)
                        }

                        if (views.layoutId == R.layout.widget_reading_progress_minimal) {
                            if (showFriendReading) {
                                views.setViewVisibility(R.id.widget_friend_reading, View.VISIBLE)
                            } else {
                                views.setViewVisibility(R.id.widget_friend_reading, View.GONE)
                            }
                        }

                        if (showPlayButton) {
                            views.setViewVisibility(R.id.widget_play_button, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.widget_play_button, View.GONE)
                        }


                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_book_cover, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_book_cover, R.drawable.book_cover_placeholder)
                        }

                        val playIntent = Intent(context, com.route.readers.ui.screens.BookReaderActivity::class.java)
                        val playPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            playIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_play_button, playPendingIntent)

                    } else {
                        views.setTextViewText(R.id.widget_book_title, "📖 읽고 있는 책이 없습니다")
                        if (views.layoutId == R.layout.widget_reading_progress) {
                            views.setTextViewText(R.id.widget_book_author, "새로운 책을 추가해보세요!")
                            views.setTextViewText(R.id.widget_page_info, "")
                        }
                        views.setTextViewText(R.id.widget_progress, "0%")
                        views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                        views.setImageViewResource(R.id.widget_book_cover, R.drawable.book_cover_placeholder)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_book_title, "데이터 로드 실패")
                    if (views.layoutId == R.layout.widget_reading_progress) {
                        views.setTextViewText(R.id.widget_book_author, "")
                        views.setTextViewText(R.id.widget_page_info, "")
                    }
                    views.setTextViewText(R.id.widget_progress, "0%")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
