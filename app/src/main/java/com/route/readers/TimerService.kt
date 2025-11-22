package com.route.readers

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import java.util.Locale

class TimerService : Service() {

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this, "TimerService created!", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "TimerService created!")
    }

    private var timerRunning = false
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val firestoreRepository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()
    private var currentBook: MyBook? = null
    private var lastLoadedBitmap: Bitmap? = null // Cache the last loaded bitmap

    // SharedPreferences keys - must match those used in TimerWidgetProvider
    private val WIDGET_PREFS_NAME = "widget_preferences"
    private val CURRENT_BOOK_ISBN_PREF = "current_reading_book_isbn_for_widget"
    private val CURRENT_BOOK_PAGE_PREF = "current_reading_book_page_for_widget"

    private val updateTimerTask = object : Runnable {
        override fun run() {
            if (timerRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                updateWidget(updateImage = false) // Don't reload image on every second update
                handler.postDelayed(this, 1000) // Update every second
            }
        }
    }

    companion object {
        const val ACTION_START_TIMER = "com.route.readers.ACTION_START_TIMER"
        const val ACTION_STOP_TIMER = "com.route.readers.ACTION_STOP_TIMER"
        const val ACTION_TOGGLE_TIMER = "com.route.readers.ACTION_TOGGLE_TIMER"
        const val ACTION_RESET_TIMER = "com.route.readers.ACTION_RESET_TIMER"
        const val ACTION_UPDATE_BOOK_DATA = "com.route.readers.ACTION_UPDATE_BOOK_DATA" // New action to refresh book data
        private const val TAG = "TimerService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "onStartCommand executed!", Toast.LENGTH_SHORT).show() // New Toast
        intent?.let {
            appWidgetId = it.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            when (it.action) {
                ACTION_TOGGLE_TIMER -> {
                    Toast.makeText(this, "Action: TOGGLE_TIMER", Toast.LENGTH_SHORT).show() // New Toast
                    if (timerRunning) {
                        stopTimer()
                    } else {
                        startTimer()
                    }
                }
                ACTION_START_TIMER -> {
                    Toast.makeText(this, "Action: START_TIMER", Toast.LENGTH_SHORT).show() // New Toast
                    startTimer()
                }
                ACTION_STOP_TIMER -> {
                    Toast.makeText(this, "Action: STOP_TIMER", Toast.LENGTH_SHORT).show() // New Toast
                    stopTimer()
                }
                ACTION_RESET_TIMER -> {
                    Toast.makeText(this, "Action: RESET_RESET_TIMER", Toast.LENGTH_SHORT).show() // New Toast
                    resetTimer()
                }
                ACTION_UPDATE_BOOK_DATA -> {
                    Toast.makeText(this, "Action: UPDATE_BOOK_DATA", Toast.LENGTH_SHORT).show() // New Toast
                    serviceScope.launch { fetchBookDataAndBitmap(updateImage = true) } // Fetch book data explicitly
                }
                else -> { // Added else to catch any unhandled actions
                    Toast.makeText(this, "Action: Unknown ${it.action}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Always try to fetch book data on service start or command
        serviceScope.launch { fetchBookDataAndBitmap(updateImage = true) }
        return START_STICKY // Service will be restarted if killed
    }

    private fun startTimer() {
        if (!timerRunning) {
            timerRunning = true
            startTime = System.currentTimeMillis() - elapsedTime // Resume from last elapsed time
            handler.post(updateTimerTask)
            Log.d(TAG, "Timer started. appWidgetId: $appWidgetId")
            updateWidgetButton(true) // Update button to pause
        }
    }

    private fun stopTimer() {
        if (timerRunning) {
            timerRunning = false
            handler.removeCallbacks(updateTimerTask)
            Log.d(TAG, "Timer stopped. appWidgetId: $appWidgetId")
            updateWidgetButton(false) // Update button to play
        }
    }

    private fun resetTimer() {
        stopTimer() // Stop the timer if it's running
        elapsedTime = 0L // Reset elapsed time
        updateWidget(updateImage = false) // Update widget to show "00:00:00" without re-loading image
        updateWidgetButton(false) // Ensure play button is shown
        Log.d(TAG, "Timer reset. appWidgetId: $appWidgetId")
    }

    private suspend fun fetchBookDataAndBitmap(updateImage: Boolean) {
        withContext(Dispatchers.Main) { Toast.makeText(this@TimerService, "Fetching book data...", Toast.LENGTH_SHORT).show() }

        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "User not logged in. Cannot fetch book data for widget.")
            withContext(Dispatchers.Main) { Toast.makeText(this@TimerService, "로그인 정보 없음", Toast.LENGTH_SHORT).show() }
            currentBook = null
            lastLoadedBitmap = null
            // Clear SharedPreferences if no user
            val sharedPrefs = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                remove(CURRENT_BOOK_ISBN_PREF)
                remove(CURRENT_BOOK_PAGE_PREF)
                apply()
            }
            withContext(Dispatchers.Main) { updateWidget(updateImage) }
            return
        }

        // --- Logic to find the currently reading book ---
        val myBooks = firestoreRepository.getMyBooks()
        currentBook = myBooks?.filter { !it.isCompleted } // Filter out completed books
            ?.sortedByDescending { it.lastReadDate } // Sort by most recent read date
            ?.firstOrNull() // Take the first (most recent) uncompleted book

        if (currentBook != null) {
            Log.d(TAG, "Found currently reading book: ${currentBook?.title}")
            withContext(Dispatchers.Main) { Toast.makeText(this@TimerService, "책 발견: ${currentBook?.title}", Toast.LENGTH_SHORT).show() }
            // Save current book info to SharedPreferences
            val sharedPrefs = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString(CURRENT_BOOK_ISBN_PREF, currentBook?.isbn)
                putInt(CURRENT_BOOK_PAGE_PREF, currentBook?.currentPage ?: 0)
                apply()
            }
        } else {
            Log.d(TAG, "No currently reading book found in library.")
            withContext(Dispatchers.Main) { Toast.makeText(this@TimerService, "현재 읽는 중인 책이 없습니다.", Toast.LENGTH_SHORT).show() }
            // Clear SharedPreferences if no book is found
            val sharedPrefs = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                remove(CURRENT_BOOK_ISBN_PREF)
                remove(CURRENT_BOOK_PAGE_PREF)
                apply()
            }
        }

        if (currentBook != null && updateImage) {
            val imageUrl = currentBook!!.getHighQualityImageUrl()
            try {
                val imageLoader = ImageLoader(this@TimerService)
                val request = ImageRequest.Builder(this@TimerService)
                    .data(imageUrl)
                    .allowHardware(false) // Disable hardware bitmaps for RemoteViews compatibility
                    .build()
                val drawable = imageLoader.execute(request).drawable
                lastLoadedBitmap = (drawable as? BitmapDrawable)?.bitmap
                withContext(Dispatchers.Main) { Toast.makeText(this@TimerService, "표지 이미지 로드 완료", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading book cover image: ${e.message}", e)
                withContext(Dispatchers.Main) { Toast.makeText(this@TimerService, "표지 이미지 로드 실패", Toast.LENGTH_SHORT).show() }
                lastLoadedBitmap = null
            }
        }
        withContext(Dispatchers.Main) { updateWidget(updateImage) } // Update widget with fetched book data (or lack thereof)
    }

    private fun updateWidget(updateImage: Boolean) { // updateImage param is now just for internal context from fetchBookDataAndBitmap
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val remoteViews = RemoteViews(packageName, R.layout.timer_widget_layout)

        // Update Timer Display
        val hours = (elapsedTime / 3600000).toInt()
        val minutes = (elapsedTime % 3600000 / 60000).toInt()
        val seconds = (elapsedTime % 60000 / 1000).toInt()
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        remoteViews.setTextViewText(R.id.timer_text_view, timeFormatted)


        // Update Book Display
        currentBook?.let { book ->
            remoteViews.setViewVisibility(R.id.book_cover_image, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.book_title_text, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.book_author_text, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.book_progress_text, android.view.View.VISIBLE)

            remoteViews.setTextViewText(R.id.book_title_text, book.title)
            remoteViews.setTextViewText(R.id.book_author_text, book.author)
            remoteViews.setTextViewText(R.id.book_progress_text, "진행률: ${book.progressPercentage}% (${book.currentPage}/${book.totalPages})")

            lastLoadedBitmap?.let {
                remoteViews.setImageViewBitmap(R.id.book_cover_image, it)
            } ?: remoteViews.setImageViewResource(R.id.book_cover_image, android.R.drawable.ic_menu_gallery)

        } ?: run {
            // No current book found, hide book details
            remoteViews.setViewVisibility(R.id.book_cover_image, android.view.View.GONE)
            remoteViews.setImageViewResource(R.id.book_cover_image, 0) // Clear any previous image
            remoteViews.setViewVisibility(R.id.book_title_text, android.view.View.GONE)
            remoteViews.setViewVisibility(R.id.book_author_text, android.view.View.GONE)
            remoteViews.setTextViewText(R.id.book_progress_text, "현재 읽는 책 없음")
            remoteViews.setViewVisibility(R.id.book_progress_text, android.view.View.VISIBLE) // Keep progress text visible for "no book" message
        }

        // Set up the PendingIntent again for the button click (play/pause)
        val toggleTimerIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TOGGLE_TIMER
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val togglePendingIntent: PendingIntent = PendingIntent.getService(
            this,
            appWidgetId, // Use appWidgetId as request code for uniqueness
            toggleTimerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.play_pause_button, togglePendingIntent)

        // Set up PendingIntent for the reset button
        val resetTimerIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_RESET_TIMER
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val resetPendingIntent: PendingIntent = PendingIntent.getService(
            this,
            appWidgetId + 1, // Use a different request code for reset
            resetTimerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.reset_button, resetPendingIntent)


        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        } else {
            val componentName = ComponentName(this, TimerWidgetProvider::class.java)
            appWidgetManager.updateAppWidget(componentName, remoteViews)
        }
        Log.d(TAG, "Widget updated: $timeFormatted")
        Toast.makeText(this@TimerService, "위젯 UI 업데이트 완료", Toast.LENGTH_SHORT).show()
    }

    private fun updateWidgetButton(isPlaying: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val remoteViews = RemoteViews(packageName, R.layout.timer_widget_layout)

        val iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        remoteViews.setImageViewResource(R.id.play_pause_button, iconRes)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews) // Use partial update for just the button
        } else {
            val componentName = ComponentName(this, TimerWidgetProvider::class.java)
            val allAppWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetManager.partiallyUpdateAppWidget(allAppWidgetIds, remoteViews)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null // This service does not allow binding
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer() // Ensure timer is stopped when service is destroyed
        serviceJob.cancel() // Cancel coroutine scope
        Log.d(TAG, "TimerService destroyed.")
    }
}