package com.route.readers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
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
import java.util.Locale
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    override fun onCreate() {
        super.onCreate()
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
    private var wasStartedByWidget: Boolean = false // Track if service was started from widget

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
        const val ACTION_UPDATE_BOOK_DATA = "com.route.readers.ACTION_UPDATE_BOOK_DATA" // New action to refresh book data
        const val EXTRA_STARTED_FROM_WIDGET = "com.route.readers.EXTRA_STARTED_FROM_WIDGET" // New extra
        private const val FOREGROUND_CHANNEL_ID = "timer_widget"
        private const val FOREGROUND_NOTIFICATION_ID = 42
        private const val TAG = "TimerService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retrieve the extra indicating if it was started from the widget
        wasStartedByWidget = intent?.getBooleanExtra(EXTRA_STARTED_FROM_WIDGET, false) ?: false
        ensureForegroundNotification(wasStartedByWidget)
        
        intent?.let {
            appWidgetId = it.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            when (it.action) {
                ACTION_TOGGLE_TIMER -> {
                    if (timerRunning) {
                        pauseTimer()
                    } else {
                        startTimer()
                    }
                }
                ACTION_START_TIMER -> {
                    startTimer()
                }
                ACTION_STOP_TIMER -> {
                    resetTimer()

                    // Launch UpdatePageCountActivity
                    val sharedPrefs = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
                    val bookIsbn = sharedPrefs.getString(CURRENT_BOOK_ISBN_PREF, null)
                    val currentPage = sharedPrefs.getInt(CURRENT_BOOK_PAGE_PREF, 0)
                    // The appWidgetId is already a member variable, but getting it from the intent is safer
                    val widgetId = it.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

                    if (bookIsbn != null && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        val updateIntent = Intent(applicationContext, UpdatePageCountActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(TimerWidgetProvider.EXTRA_BOOK_ISBN, bookIsbn)
                            putExtra(TimerWidgetProvider.EXTRA_CURRENT_PAGE, currentPage)
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }
                        startActivity(updateIntent)
                    }
                }
                ACTION_UPDATE_BOOK_DATA -> {
                    val targetIds = it.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                        ?: intArrayOf(appWidgetId)
                    serviceScope.launch {
                        targetIds.forEach { id ->
                            appWidgetId = id
                            fetchBookDataAndBitmap(updateImage = true)
                        }
                    } // Fetch book data explicitly
                }
                else -> { // Added else to catch any unhandled actions
                }
            }
        }
        // Always try to fetch book data on service start or command
        serviceScope.launch { fetchBookDataAndBitmap(updateImage = true) }
        return START_STICKY // Service will be restarted if killed
    }

    @SuppressLint("ForegroundServiceType")
    private fun ensureForegroundNotification(shouldShowNotification: Boolean) {
        if (!shouldShowNotification) {
            stopForeground(true)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Reading Timer",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("독서 타이머 실행 중")
            .setContentText("위젯을 눌러 진행 상황을 확인하세요.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer() {
        if (!timerRunning) {
            timerRunning = true
            startTime = System.currentTimeMillis() - elapsedTime // Resume from last elapsed time
            handler.post(updateTimerTask)
            Log.d(TAG, "Timer started. appWidgetId: $appWidgetId")
            updateWidget(updateImage = false) // Update widget with new button state
        }
    }

    private fun pauseTimer() {
        if (timerRunning) {
            timerRunning = false
            handler.removeCallbacks(updateTimerTask)
            Log.d(TAG, "Timer paused. appWidgetId: $appWidgetId")
            updateWidget(updateImage = false) // Update widget with new button state
        }
    }

    private fun resetTimer() {
        timerRunning = false
        handler.removeCallbacks(updateTimerTask)
        elapsedTime = 0L // Reset elapsed time
        Log.d(TAG, "Timer reset. appWidgetId: $appWidgetId")
        updateWidget(updateImage = false) // Update widget to show "00:00:00"
    }



    private suspend fun fetchBookDataAndBitmap(updateImage: Boolean) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "User not logged in. Cannot fetch book data for widget.")
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

        val sharedPrefs = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        val savedIsbn = sharedPrefs.getString(CURRENT_BOOK_ISBN_PREF, null)

        val myBooks = firestoreRepository.getMyBooks()

        currentBook = if (savedIsbn != null) {
            myBooks?.find { it.isbn == savedIsbn }
        } else {
            myBooks?.filter { !it.isCompleted } // Filter out completed books
                ?.sortedByDescending { it.lastReadDate } // Sort by most recent read date
                ?.firstOrNull() // Take the first (most recent) uncompleted book
        }

        if (currentBook != null) {
            Log.d(TAG, "Found currently reading book: ${currentBook?.title}")
            // Save current book info to SharedPreferences
            with(sharedPrefs.edit()) {
                putString(CURRENT_BOOK_ISBN_PREF, currentBook?.isbn)
                putInt(CURRENT_BOOK_PAGE_PREF, currentBook?.currentPage ?: 0)
                apply()
            }
        } else {
            Log.d(TAG, "No currently reading book found in library.")
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
            } catch (e: Exception) {
                Log.e(TAG, "Error loading book cover image: ${e.message}", e)
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

        // Update Play/Pause Button Icon
        val iconRes = if (timerRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        remoteViews.setImageViewResource(R.id.play_pause_button, iconRes)


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


        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        } else {
            val componentName = ComponentName(this, TimerWidgetProvider::class.java)
            appWidgetManager.updateAppWidget(componentName, remoteViews)
        }
        Log.d(TAG, "Widget updated: $timeFormatted")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // This service does not allow binding
    }

    override fun onDestroy() {
        super.onDestroy()
        if (timerRunning) pauseTimer() // Ensure timer state is saved when service is destroyed
        serviceJob.cancel() // Cancel coroutine scope
        stopForeground(true)
        Log.d(TAG, "TimerService destroyed.")
    }
}
