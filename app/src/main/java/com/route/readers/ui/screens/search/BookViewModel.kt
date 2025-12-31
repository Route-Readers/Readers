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

    private val _selectedSort = MutableStateFlow("정확도순")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()


    val availableGenres: Map<String, List<String>> = linkedMapOf(
        "소설" to listOf("소설", "한국소설", "영미소설", "일본소설", "과학소설(SF)", "추리/미스터리", "판타지/무협", "로맨스"),
        "교양" to listOf("시/에세이", "인문", "사회", "역사"),
        "실용" to listOf("과학", "기술", "경제/경영", "자기계발"),
        "기타" to listOf("예술", "종교", "여행", "어린이", "청소년", "만화")
    )

    val availableSorts = listOf("정확도순", "출간일순", "고객평점순", "베스트셀러")

    private val aladinSortValues = mapOf(
        "정확도순" to "Accuracy",
        "출간일순" to "PublishTime",
        "고객평점순" to "CustomerRating",
        "베스트셀러" to "BestsellerQueryType" // "베스트셀러"는 QueryType으로 사용될 것이므로 다른 값으로 매핑
    )


    private val aladinGenreCategoryIds = mapOf(
        // 소설 (세부 장르)
        "소설" to "1", // 소설/시/희곡의 최상위 ID, 더 구체적인 ID를 사용할 것을 권장
        "한국소설" to "50973", // 확인된 ID
        "영미소설" to "50978", // 추정 ID
        "일본소설" to "50998", // 확인된 ID
        "과학소설(SF)" to "50992", // 확인된 ID
        "추리/미스터리" to "50982", // 추정 ID
        "판타지/무협" to "50988", // 추정 ID
        "로맨스" to "50976", // 확인된 ID

        "시/에세이" to "1700",
        "인문" to "1200",
        "사회" to "798",
        "역사" to "1600",
        "과학" to "987",
        "기술" to "2300",
        "경제/경영" to "656", // 경제경영 최상위 ID
        "자기계발" to "1380",
        "예술" to "517",
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
        performSearch(_currentQuery.value, selectedGenres = _selectedGenres.value, sort = _selectedSort.value, isNewSearch = true)
    }

    fun onSortSelected(sort: String) {
        _selectedSort.value = sort
        performSearch(_currentQuery.value, selectedGenres = _selectedGenres.value, sort = _selectedSort.value, isNewSearch = true)
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
                    performSearch(query, selectedGenres = _selectedGenres.value, sort = _selectedSort.value, isNewSearch = true)
                }
        }
    }

    private suspend fun applyFavoriteStatusToBooks(books: List<Book>): List<Book> {
        val wishlistIsbns = wishlistRepository.getWishlist().toSet()
        return books.map { book ->
            book.copy(isFavorite = wishlistIsbns.contains(book.isbn))
        }
    }

    private fun matchesGenre(categoryName: String?, filterGenre: String): Boolean {
        if (categoryName == null) return false
        val cleanedCategoryName = if (categoryName.startsWith("국내도서>")) {
            categoryName.substringAfter("국내도서>")
        } else {
            categoryName
        }
        // Check for exact match or starts with the genre followed by '>'
        return cleanedCategoryName == filterGenre || cleanedCategoryName.startsWith("$filterGenre>")
    }

    fun searchBooks(query: String) {
        _currentQuery.value = query
    }

    fun performSearch(query: String, selectedGenres: List<String> = emptyList(), sort: String = "정확도순", isNewSearch: Boolean = true) {
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
                val sortValue = aladinSortValues[sort] ?: "Accuracy"
                var clientFilterGenres: List<String> = emptyList()
                var apiGenres: List<String> = selectedGenres
                var apiQuery: String = query.trim()

                val apiResult: List<Book> = if (sortValue == "BestsellerQueryType") {
                    // "베스트셀러" 정렬 시 쿼리 대신 ItemList API 호출
                    // _currentQuery.value를 빈 문자열로 설정하여 키워드 검색을 비활성화
                    _currentQuery.value = ""
                    // 베스트셀러는 장르 필터링을 적용하지 않으므로 clientFilterGenres와 apiGenres를 비워둡니다.
                    clientFilterGenres = emptyList()
                    apiGenres = emptyList()
                    bookRepository.getBestsellerList(page = currentPage, maxResults = pageSize)
                } else {
                    // 기존 ItemSearch API 호출
                    apiQuery = if (query.isBlank()) {
                        if (selectedGenres.size == 1) {
                            selectedGenres.first().trim()
                        } else {
                            "책"
                        }
                    } else {
                        query.trim()
                    }

                    // If a genre has an Aladin ID, it should be filtered by the API first.
                    // Only use client-side filtering for genres that do NOT have a direct Aladin category ID.
                    val (clientFiltered, apiFiltered) = selectedGenres.partition { genre: String ->
                        !aladinGenreCategoryIds.containsKey(genre)
                    }
                    clientFilterGenres = clientFiltered
                    apiGenres = apiFiltered

                    // Get Aladin Category IDs from selected genres that have 'certain' IDs
                    val apiCategoryIds = apiGenres.mapNotNull { genre: String ->
                        aladinGenreCategoryIds[genre]
                    }.joinToString(",")

                    bookRepository.getBookSearch(
                        query = apiQuery,
                        page = currentPage,
                        maxResults = pageSize,
                        categoryId = if (apiCategoryIds.isNotBlank()) apiCategoryIds else null,
                        sort = sortValue
                    )
                }
                
                val newBooks = applyFavoriteStatusToBooks(apiResult)

                // Apply client-side filtering for the 'uncertain' genres
                val filteredBooks = if (clientFilterGenres.isNotEmpty()) {
                    newBooks.filter { book ->
                        clientFilterGenres.any { genre ->
                            matchesGenre(book.categoryName, genre)
                        }
                    }
                } else {
                    newBooks
                }

                val finalBooks = filteredBooks

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
        performSearch(_currentQuery.value, selectedGenres = _selectedGenres.value, sort = _selectedSort.value, isNewSearch = false)
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
