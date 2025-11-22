package com.route.readers

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookSelectionActivity : AppCompatActivity() {

    private val firestoreRepository = FirestoreRepository()
    private var activityJob = Job()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob) // Main dispatcher for UI

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_selection) // We will create this layout

        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "위젯 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.book_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        bookAdapter = BookAdapter { selectedBook ->
            // Handle book selection
            saveSelectedBookToWidget(selectedBook)
            finish()
        }
        recyclerView.adapter = bookAdapter

        fetchMyBooks()
    }

    private fun fetchMyBooks() {
        activityScope.launch {
            val myBooks = firestoreRepository.getMyBooks()
            val currentlyReadingBooks = myBooks?.filter { !it.isCompleted } // Filter for uncompleted books
            withContext(Dispatchers.Main) {
                if (!currentlyReadingBooks.isNullOrEmpty()) {
                    bookAdapter.submitList(currentlyReadingBooks)
                } else {
                    Toast.makeText(this@BookSelectionActivity, "현재 읽는 중인 책이 없습니다.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun saveSelectedBookToWidget(book: MyBook) {
        val sharedPrefs = getSharedPreferences(TimerWidgetProvider.WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(TimerWidgetProvider.CURRENT_BOOK_ISBN_PREF, book.isbn)
            putInt(TimerWidgetProvider.CURRENT_BOOK_PAGE_PREF, book.currentPage) // Also save current page
            apply()
        }

        // Trigger widget update
        val updateIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE_BOOK_DATA
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        startService(updateIntent)
        Toast.makeText(this, "'${book.title}' 위젯에 반영되었습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel() // Cancel coroutine scope
    }
}

// RecyclerView Adapter
class BookAdapter(private val onItemClick: (MyBook) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<MyBook, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_selection, parent, false) // We will create this layout
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverImageView: ImageView = itemView.findViewById(R.id.book_selection_cover_image)
        private val titleTextView: TextView = itemView.findViewById(R.id.book_selection_title_text)
        private val authorTextView: TextView = itemView.findViewById(R.id.book_selection_author_text)
        private val progressTextView: TextView = itemView.findViewById(R.id.book_selection_progress_text)

        init {
            itemView.setOnClickListener {
                onItemClick(getItem(adapterPosition))
            }
        }

        fun bind(book: MyBook) {
            coverImageView.load(book.getHighQualityImageUrl()) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
            titleTextView.text = book.title
            authorTextView.text = book.author
            progressTextView.text = "현재 ${book.currentPage} / ${book.totalPages} 페이지 (진행률: ${book.progressPercentage}%)"
        }
    }
}

class BookDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<MyBook>() {
    override fun areItemsTheSame(oldItem: MyBook, newItem: MyBook): Boolean {
        return oldItem.isbn == newItem.isbn
    }

    override fun areContentsTheSame(oldItem: MyBook, newItem: MyBook): Boolean {
        return oldItem == newItem
    }
}
