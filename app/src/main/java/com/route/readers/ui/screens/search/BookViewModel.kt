package com.route.readers.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.Book
import com.route.readers.data.remote.BookRepository
import com.route.readers.data.remote.WishlistRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class BookViewModel : ViewModel() {

    private val bookRepository = BookRepository()
    private val wishlistRepository = WishlistRepository()

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

    init {
        viewModelScope.launch {
            _currentQuery
                .debounce(500)
                .filter { it.isNotBlank() }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query, isNewSearch = true)
                }
        }
    }

    private suspend fun applyFavoriteStatusToBooks(books: List<Book>): List<Book> {
        val wishlistIsbns = wishlistRepository.getWishlist().toSet()
        return books.map { book ->
            book.copy(isFavorite = wishlistIsbns.contains(book.isbn))
        }
    }

    fun searchBooks(query: String) {
        _currentQuery.value = query
    }

    private fun performSearch(query: String, isNewSearch: Boolean = true) {
        if (query.isBlank()) {
            _books.value = emptyList()
            return
        }

        if (isNewSearch) {
            currentPage = 1
        }

        viewModelScope.launch {
            if (isNewSearch) {
                _isLoading.value = true
                _books.value = emptyList()
            } else {
                _isLoadingMore.value = true
            }
            _errorMessage.value = null

            try {
                val result = bookRepository.getBookSearch(query.trim(), currentPage, pageSize)
                val newBooks = applyFavoriteStatusToBooks(result)

                if (isNewSearch) {
                    _books.value = newBooks
                } else {
                    _books.value = _books.value + newBooks
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
        performSearch(_currentQuery.value, false)
    }

    fun getNewBooks() {
        _currentQuery.value = ""
        currentPage = 1

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = bookRepository.getBookList()
                _books.value = applyFavoriteStatusToBooks(result)
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
                if (newFavoriteStatus) {
                    wishlistRepository.addToWishlist(book)
                } else {
                    wishlistRepository.removeFromWishlist(book.isbn)
                }
                applyFavoriteStatusToBooks(_books.value)
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
