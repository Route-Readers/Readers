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

    private val _selectedGenres = MutableStateFlow<List<String>>(emptyList())
    val selectedGenres: StateFlow<List<String>> = _selectedGenres.asStateFlow()

    val availableGenres = listOf(
        "소설", "시/에세이", "인문", "사회", "역사", "과학", "기술", "예술", "자기계발", "종교", "여행", "어린이", "청소년", "만화"
    )

    fun onGenreSelected(genre: String) {
        val currentSelection = _selectedGenres.value.toMutableList()
        if (currentSelection.contains(genre)) {
            currentSelection.remove(genre)
        } else {
            currentSelection.add(genre)
        }
        _selectedGenres.value = currentSelection
        performSearch(_currentQuery.value, selectedGenres = _selectedGenres.value, isNewSearch = true)
    }

    private var currentPage = 1
    private val pageSize = 10

    init {
        viewModelScope.launch {
            _currentQuery
                .debounce(500)
                .filter { it.isNotBlank() }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query, selectedGenres = _selectedGenres.value, isNewSearch = true)
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

    fun performSearch(query: String, selectedGenres: List<String> = emptyList(), isNewSearch: Boolean = true) {
        if (query.isBlank() && selectedGenres.isEmpty()) {
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
                // Determine whether to use getBookSearch (with query) or getBookList (for new books)
                val apiResult = if (query.isBlank() && selectedGenres.isNotEmpty()) {
                    // If no query but genres are selected, fetch new books and filter
                    bookRepository.getBookList()
                } else {
                    // If there's a query, use book search API
                    bookRepository.getBookSearch(query.trim(), currentPage, pageSize)
                }

                val newBooks = applyFavoriteStatusToBooks(apiResult)

                val filteredBooks = if (selectedGenres.isNotEmpty()) {
                    newBooks.filter { book ->
                        val fullCategoryName = book.categoryName ?: ""
                        selectedGenres.any { genre ->
                            fullCategoryName.contains(genre, ignoreCase = true) ||
                            fullCategoryName.split(">", ",").any { part ->
                                part.trim().contains(genre, ignoreCase = true)
                            }
                        }
                    }
                } else {
                    newBooks
                }

                if (isNewSearch) {
                    _books.value = filteredBooks
                } else {
                    _books.value = _books.value + filteredBooks
                }

                // _hasMoreResults logic needs adjustment if getBookList is used, as it doesn't support pagination.
                // For getBookList, we assume no more results for now.
                _hasMoreResults.value = if (query.isBlank() && selectedGenres.isNotEmpty()) {
                    false // getBookList doesn't paginate, so no more results
                } else {
                    apiResult.size >= pageSize
                }

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
        performSearch(_currentQuery.value, selectedGenres = _selectedGenres.value, false)
    }

    fun getNewBooks() {
        _currentQuery.value = ""
        currentPage = 1

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = bookRepository.getBookList()
                val newBooks = applyFavoriteStatusToBooks(result)

                val filteredBooks = if (_selectedGenres.value.isNotEmpty()) {
                    newBooks.filter { book ->
                        _selectedGenres.value.any { genre -> book.categoryName?.contains(genre) ?: false }
                    }
                } else {
                    newBooks
                }

                _books.value = filteredBooks
                _hasMoreResults.value = false
                if (filteredBooks.isEmpty()) {
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
