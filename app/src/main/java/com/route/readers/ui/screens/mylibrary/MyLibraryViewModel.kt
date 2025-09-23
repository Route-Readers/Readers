package com.route.readers.ui.screens.mylibrary

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.route.readers.data.model.Book

class MyLibraryViewModel : ViewModel() {
    
    private val _books = MutableStateFlow(
        listOf(
            Book(title = "아토믹 해빗", author = "제임스 클리어", currentPage = 47, totalPages = 320, progress = 15),
            Book(title = "사피엔스", author = "유발 하라리", currentPage = 123, totalPages = 435, progress = 28),
            Book(title = "데미안", author = "헤르만 헤세", currentPage = 78, totalPages = 240, progress = 33)
        )
    )
    val books: StateFlow<List<Book>> = _books.asStateFlow()
    
    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()
    
    fun startReading() {
        _isReading.value = true
    }
    
    fun stopReading() {
        _isReading.value = false
    }
    
    fun addBook(book: Book) {
        _books.value = _books.value + book
    }
}
