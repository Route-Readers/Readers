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

    private val genreCategoryMap = mapOf(
        "소설" to listOf("소설", "장르소설"),
        "시/에세이" to listOf("시", "에세이"),
        "인문" to listOf("인문학", "인문"),
        "사회" to listOf("사회과학", "사회"),
        "역사" to listOf("역사"),
        "과학" to listOf("과학"),
        "기술" to listOf("컴퓨터", "IT", "기술"), // Broader terms for "기술"
        "예술" to listOf("예술", "대중문화"),
        "자기계발" to listOf("자기계발"),
        "종교" to listOf("종교"),
        "여행" to listOf("여행"),
        "어린이" to listOf("어린이", "유아", "아동"),
        "청소년" to listOf("청소년"),
        "만화" to listOf("만화", "코믹")
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

                val apiResult = bookRepository.getBookSearch(apiQuery, currentPage, pageSize)
                val newBooks = applyFavoriteStatusToBooks(apiResult)

                val filteredBooks = if (selectedGenres.isNotEmpty()) {
                    newBooks.filter { book ->
                        val fullCategoryName = book.categoryName ?: ""
                        val fullCategoryNameLower = fullCategoryName.lowercase()
                        val categoryPartsLower = fullCategoryName.split(">", ",").map { it.trim().lowercase() }

                        selectedGenres.any { genre ->
                            val targetAladinCategories = genreCategoryMap[genre] ?: listOf(genre) // Get mapped categories or use genre itself

                            targetAladinCategories.any { targetCategory ->
                                val targetCategoryLower = targetCategory.lowercase()
                                // Check if full category name contains the target category
                                fullCategoryNameLower.contains(targetCategoryLower) ||
                                // Check if any part of the split category name contains the target category
                                categoryPartsLower.any { part -> part.contains(targetCategoryLower) }
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
                
                // _hasMoreResults should be based on the actual API result size, regardless of API query
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
