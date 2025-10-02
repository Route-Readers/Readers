package com.route.readers.data.remote

import com.route.readers.data.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MyLibraryRepository {
    
    private val _myBooks = MutableStateFlow<List<Book>>(emptyList())
    val myBooks: StateFlow<List<Book>> = _myBooks
    
    fun addBookToLibrary(book: Book) {
        val currentBooks = _myBooks.value.toMutableList()
        // 이미 있는 책인지 확인 (ISBN으로 중복 체크)
        if (currentBooks.none { it.isbn == book.isbn }) {
            // 페이지 정보 추출
            val totalPages = book.getPageCount().takeIf { it > 0 } ?: 300
            
            val bookWithProgress = book.copy(
                totalPages = totalPages,
                currentPage = 0,
                progress = 0
            )
            currentBooks.add(bookWithProgress)
            _myBooks.value = currentBooks
        }
    }
    
    fun updateReadingProgress(isbn: String, currentPage: Int) {
        val currentBooks = _myBooks.value.toMutableList()
        val bookIndex = currentBooks.indexOfFirst { it.isbn == isbn }
        
        if (bookIndex != -1) {
            val book = currentBooks[bookIndex]
            val progress = if (book.totalPages > 0) {
                ((currentPage.toFloat() / book.totalPages) * 100).toInt()
            } else 0
            
            currentBooks[bookIndex] = book.copy(
                currentPage = currentPage,
                progress = progress
            )
            _myBooks.value = currentBooks
        }
    }
    
    fun removeBookFromLibrary(isbn: String) {
        val currentBooks = _myBooks.value.toMutableList()
        currentBooks.removeAll { it.isbn == isbn }
        _myBooks.value = currentBooks
    }
    
    fun isBookInLibrary(isbn: String): Boolean {
        return _myBooks.value.any { it.isbn == isbn }
    }
}
