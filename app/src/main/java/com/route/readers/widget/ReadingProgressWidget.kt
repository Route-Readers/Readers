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
import com.route.readers.service.ReadingTimerService
import com.route.readers.ui.screens.profile.WidgetColorScheme
import com.route.readers.ui.screens.profile.WidgetStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.util.Log

class ReadingProgressWidget : AppWidgetProvider() { // Class definition starts here

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun formatTime(timeInMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
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

                        // --- Timer Display Logic ---
                        val sharedPrefs = context.getSharedPreferences(ReadingTimerService.PREFS_NAME, Context.MODE_PRIVATE)
                        val savedElapsedTime = sharedPrefs.getLong("${currentBook.id}-elapsedTime", 0L)
                        val isTimerRunningForThisBook = sharedPrefs.getBoolean("${currentBook.id}-isTimerRunning", false)

                        views.setTextViewText(R.id.widget_reading_timer, formatTime(savedElapsedTime))
                        views.setViewVisibility(R.id.widget_reading_timer, View.VISIBLE)
                        // --- End Timer Display Logic ---

                        if (showPlayButton) {
                            views.setViewVisibility(R.id.widget_play_button, View.VISIBLE)
                            val playIntent = Intent(context, com.route.readers.service.ReadingTimerService::class.java).apply {
                                action = com.route.readers.service.ReadingTimerService.ACTION_TOGGLE_TIMER
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                putExtra(com.route.readers.service.ReadingTimerService.EXTRA_BOOK_ID, currentBook.id)
                            }
                            val playPendingIntent = PendingIntent.getService(
                                context,
                                appWidgetId,
                                playIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widget_play_button, playPendingIntent)

                            // Explicitly set container's click listener to null if play button is active
                            views.setOnClickPendingIntent(R.id.widget_container, null)

                            // Set play/pause icon based on timer state
                            val playPauseIcon = if (isTimerRunningForThisBook) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            views.setImageViewResource(R.id.widget_play_button, playPauseIcon)

                        } else {
                            views.setViewVisibility(R.id.widget_play_button, View.GONE)
                        }

                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_book_cover, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_book_cover, R.drawable.book_cover_placeholder)
                        }

                        // If currentBook exists, the play button handles clicks.
                        // The widget container should not launch MainActivity if a play button is visible.
                        // No need to set setOnClickPendingIntent for R.id.widget_container here.

                    } else { // currentBook == null
                        views.setTextViewText(R.id.widget_book_title, "📖 읽고 있는 책이 없습니다")
                        if (views.layoutId == R.layout.widget_reading_progress) {
                            views.setTextViewText(R.id.widget_book_author, "새로운 책을 추가해보세요!")
                            views.setTextViewText(R.id.widget_page_info, "")
                        }
                        views.setTextViewText(R.id.widget_progress, "0%")
                        views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                        views.setImageViewResource(R.id.widget_book_cover, R.drawable.book_cover_placeholder)
                        views.setViewVisibility(R.id.widget_play_button, View.GONE)
                        views.setViewVisibility(R.id.widget_reading_timer, View.GONE)

                        // If no book, set the entire widget container to launch MainActivity
                        val intent = Intent(context, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(
                            context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
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
} // Class definition ends here