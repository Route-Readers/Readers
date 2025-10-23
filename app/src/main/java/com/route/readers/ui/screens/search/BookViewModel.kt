package com.route.readers.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.Book
import com.route.readers.data.remote.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookViewModel : ViewModel() {

    private val bookRepository = BookRepository()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    private val _hasMoreResults = MutableStateFlow(false)
    val hasMoreResults: StateFlow<Boolean> = _hasMoreResults.asStateFlow()

    private var currentPage = 1
    private val pageSize = 10

    fun searchBooks(query: String, isNewSearch: Boolean = true) {
        if (query.isBlank()) {
            return
        }

        if (isNewSearch) {
            currentPage = 1
            _currentQuery.value = query
            _books.value = emptyList()
        }

        viewModelScope.launch {
            if (isNewSearch) {
                _isLoading.value = true
            } else {
                _isLoadingMore.value = true
            }
            _errorMessage.value = null

            try {
                val result = bookRepository.getBookSearch(query.trim(), currentPage, pageSize)

                if (isNewSearch) {
                    _books.value = result
                } else {
                    _books.value = _books.value + result
                }

                _hasMoreResults.value = result.size >= pageSize

            } catch (e: Exception) {
                _errorMessage.value = "검색 중 오류가 발생했습니다: ${e.message}"
                if (isNewSearch) {
                    _books.value = emptyList()
                }
            } finally {
                _isLoading.value = false
                _isLoadingMore.value = false
            }
        }
    }

    fun loadMoreBooks() {
        if (_isLoadingMore.value || !_hasMoreResults.value) return

        currentPage++
        searchBooks(_currentQuery.value, false)
    }

    fun getNewBooks() {
        _currentQuery.value = ""
        currentPage = 1

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = bookRepository.getBookList()
                _books.value = result
                _hasMoreResults.value = false
                if (result.isEmpty()) {
                    _errorMessage.value = "신간 도서를 불러올 수 없습니다"
                }
            } catch (e: Exception) {
                _errorMessage.value = "신간 도서 불러오기 중 오류: ${e.message}"
                _books.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onToggleFavorite(book: Book) {
        val originalBooks = _books.value
        val newFavoriteStatus = !book.isFavorite

        _books.value = originalBooks.map {
            if (it.isbn == book.isbn) {
                it.copy(isFavorite = newFavoriteStatus)
            } else {
                it
            }
        }

        viewModelScope.launch {
            try {
                val bookToUpdate = book.copy(isFavorite = newFavoriteStatus)
                bookRepository.toggleFavoriteStatus(bookToUpdate)
            } catch (e: Exception) {
                _books.value = originalBooks
            }
        }
    }

    fun clearSearchResults() {
        _books.value = emptyList()
        _currentQuery.value = ""
        _errorMessage.value = null
        _hasMoreResults.value = false
        currentPage = 1
    }
}
