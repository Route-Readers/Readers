package com.route.readers.ui.screens.search

import android.util.Log
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
            Log.w("BookViewModel", "Empty query provided")
            return
        }

        if (isNewSearch) {
            currentPage = 1
            _currentQuery.value = query
            _books.value = emptyList()
        }

        Log.d("BookViewModel", "검색 시작: $query, 페이지: $currentPage")
        viewModelScope.launch {
            if (isNewSearch) {
                _isLoading.value = true
            } else {
                _isLoadingMore.value = true
            }
            _errorMessage.value = null

            try {
                Log.d("BookViewModel", "API 호출 중...")
                val result = bookRepository.getBookSearch(query.trim(), currentPage, pageSize)
                Log.d("BookViewModel", "검색 결과: ${result.size}개")

                if (isNewSearch) {
                    _books.value = result
                } else {
                    _books.value = _books.value + result
                }

                _hasMoreResults.value = result.size >= pageSize

                if (result.isEmpty() && isNewSearch) {
                    _errorMessage.value = "검색 결과가 없습니다"
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "검색 에러: ${e.message}", e)
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
        Log.d("BookViewModel", "신간 도서 불러오기 시작")
        _currentQuery.value = ""
        currentPage = 1

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                Log.d("BookViewModel", "신간 API 호출 중...")
                val result = bookRepository.getBookList()
                Log.d("BookViewModel", "신간 결과: ${result.size}개")
                _books.value = result
                _hasMoreResults.value = false
                if (result.isEmpty()) {
                    _errorMessage.value = "신간 도서를 불러올 수 없습니다"
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "신간 에러: ${e.message}", e)
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
                Log.e("BookViewModel", "Failed to toggle favorite status on server, rolling back UI.", e)
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
