package com.route.readers

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdatePageCountActivity : AppCompatActivity() {

    private val firestoreRepository = FirestoreRepository()
    private var activityJob = Job()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob) // Main dispatcher for UI

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var bookIsbn: String? = null
    private var currentPage: Int = 0
    private var totalPages: Int = 0 // New member variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_page_count) // We will create this layout

        bookIsbn = intent.getStringExtra(TimerWidgetProvider.EXTRA_BOOK_ISBN)
        currentPage = intent.getIntExtra(TimerWidgetProvider.EXTRA_CURRENT_PAGE, 0)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (bookIsbn == null) {
            Toast.makeText(this, "현재 읽는 책 정보를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val bookTitleTextView: TextView = findViewById(R.id.book_title_update_text)
        val currentPageTextView: TextView = findViewById(R.id.current_page_text)
        val newPageEditText: EditText = findViewById(R.id.new_page_edit_text)
        val updateButton: Button = findViewById(R.id.update_page_button)
        val cancelButton: Button = findViewById(R.id.cancel_page_button)


        currentPageTextView.text = "현재 페이지: $currentPage"
        newPageEditText.setText(currentPage.toString()) // Pre-fill with current page

        // Fetch book title (and total pages) for display
        activityScope.launch {
            val myBooks = firestoreRepository.getMyBooks()
            val book = myBooks?.firstOrNull { it.isbn == bookIsbn }
            withContext(Dispatchers.Main) {
                if (book != null) {
                    bookTitleTextView.text = "${book.title} - ${book.author} (총 ${book.totalPages} 페이지)"
                    totalPages = book.totalPages // Store total pages
                } else {
                    bookTitleTextView.text = "책 정보 불러오기 실패 (ISBN: $bookIsbn)"
                }
            }
        }


        updateButton.setOnClickListener {
            val newPageText = newPageEditText.text.toString()
            val newPage = newPageText.toIntOrNull()

            if (newPage == null || newPage < 0) {
                Toast.makeText(this, "유효한 페이지 번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPage < currentPage) {
                Toast.makeText(this, "새 페이지는 현재 페이지보다 작을 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPage > totalPages && totalPages > 0) {
                Toast.makeText(this, "새 페이지는 총 페이지 수를 초과할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Determine if the book is completed with this update
            val isCompleted = (newPage == totalPages && totalPages > 0)

            activityScope.launch {
                val success = firestoreRepository.updateReadingProgress(bookIsbn!!, newPage, isCompleted)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@UpdatePageCountActivity, if (isCompleted) "책을 완료했습니다!" else "페이지 업데이트 완료!", Toast.LENGTH_SHORT).show()
                        // Stop timer and trigger widget update
                        stopTimerAndRefreshWidget()
                        finish()
                    } else {
                        Toast.makeText(this@UpdatePageCountActivity, "페이지 업데이트 실패.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        cancelButton.setOnClickListener {
            finish() // Just close the activity
        }
    }

    private fun stopTimerAndRefreshWidget() {
        // 1. Stop the timer service
        val stopServiceIntent = Intent(this@UpdatePageCountActivity, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_TIMER
        }
        this@UpdatePageCountActivity.startService(stopServiceIntent)

        // 2. Trigger widget update
        val updateIntent = Intent(this@UpdatePageCountActivity, TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE_BOOK_DATA
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        this@UpdatePageCountActivity.startService(updateIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel() // Cancel coroutine scope
    }
}
