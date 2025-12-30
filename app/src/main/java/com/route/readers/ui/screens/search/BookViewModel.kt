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
        "소설", "국내소설", "외국소설", // Added more granular novel categories
        "시/에세이", "인문", "사회", "역사", "과학", "기술", "예술", "자기계발", "종교", "여행", "어린이", "청소년", "만화"
    )

    private val aladinGenreCategoryIds = mapOf(
        "소설" to "1100", // General Novel - might need to be removed or mapped to a combination of sub-genres
        "국내소설" to "1101", // Placeholder ID for Korean Novel
        "외국소설" to "1102", // Placeholder ID for Foreign Novel
        "시/에세이" to "1700",
        "인문" to "1200",
        "사회" to "798",
        "역사" to "1600",
        "과학" to "987",
        "기술" to "2300",
        "예술" to "517",
        "자기계발" to "1380",
        "종교" to "1800",
        "여행" to "1900",
        "어린이" to "74",
        "청소년" to "76",
        "만화" to "2550"
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
        // If both query and selectedGenres are empty, and it's NOT an initial search (i.e., user cleared everything)
        // then we can clear the books. For an initial search (isNewSearch = true), we want to show default "책"
        if (query.isBlank() && selectedGenres.isEmpty() && !isNewSearch) {
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
                // Determine the actual query to send to the API
                val apiQuery = if (query.isBlank()) {
                    // If the user's query is blank, use a broad default term to get results from search API
                    // This addresses the user's request to apply filters to "all books" (or a broad set)
                    // and not just "new books", when no specific search term is provided.
                    "책" // A very general term to fetch a broad range of books from the search API
                } else {
                    query.trim()
                }

                // Get Aladin Category IDs from selected genres
                val categoryIds = selectedGenres.mapNotNull { genre ->
                    aladinGenreCategoryIds[genre]
                }.joinToString(",")

                val apiResult = bookRepository.getBookSearch(
                    apiQuery,
                    currentPage,
                    pageSize,
                    if (categoryIds.isNotBlank()) categoryIds else null // Pass null if no categories selected
                )
                val newBooks = applyFavoriteStatusToBooks(apiResult)

                // *** REMOVE CLIENT-SIDE GENRE FILTERING ***
                // Since categoryId is passed to the API, the results should already be filtered by genre.
                // No need for a separate 'filteredBooks' variable and its logic.
                val finalBooks = newBooks

                if (isNewSearch) {
                    _books.value = finalBooks
                } else {
                    _books.value = _books.value + finalBooks
                }
                
                _hasMoreResults.value = apiResult.size >= pageSize

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
