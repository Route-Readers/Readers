package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.Book
import com.route.readers.data.remote.BookRepository
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Goal(
    val bookTitle: String = "",
    val bookIsbn: String = "",
    val bookCover: String = "",
    val duration: String = "",
    val pages: String = ""
)

data class GoalUiState(
    val bookTitleInput: String = "",
    val durationInput: String = "",
    val pagesInput: String = "",
    val showGoalInputs: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Book> = emptyList(),
    val isSearching: Boolean = false,
    val selectedBook: Book? = null
)

class GoalViewModel : ViewModel() {

    private val firestoreRepository = FirestoreRepository()
    private val bookRepository = BookRepository()

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadGoals()
    }

    private fun loadGoals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val goals = firestoreRepository.getGoals()
            _uiState.update { it.copy(goals = goals, isLoading = false) }
        }
    }

    fun onBookTitleChange(newTitle: String) {
        _uiState.update { it.copy(bookTitleInput = newTitle, selectedBook = null) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isNotBlank()) {
            _uiState.update { it.copy(isSearching = true) }
            searchJob = viewModelScope.launch {
                delay(500)
                try {
                    val books = bookRepository.getBookSearch(query, 1, 10)
                    _uiState.update { it.copy(searchResults = books, isSearching = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "책 검색에 실패했습니다.", isSearching = false) }
                }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun onBookSelected(book: Book) {
        _uiState.update {
            it.copy(
                selectedBook = book,
                searchQuery = book.title,
                bookTitleInput = book.title,
                pagesInput = book.itemPage.toString(),
                searchResults = emptyList()
            )
        }
    }

    fun onDurationChange(newDuration: String) {
        _uiState.update { it.copy(durationInput = newDuration) }
    }

    fun onPagesChange(newPages: String) {
        _uiState.update { it.copy(pagesInput = newPages) }
    }

    fun onShowGoalInputs(show: Boolean) {
        if (!show) {
            _uiState.update {
                it.copy(
                    showGoalInputs = false,
                    searchQuery = "",
                    searchResults = emptyList(),
                    selectedBook = null,
                    bookTitleInput = "",
                    durationInput = "",
                    pagesInput = ""
                )
            }
        } else {
            _uiState.update { it.copy(showGoalInputs = true) }
        }
    }

    fun saveGoal() {
        viewModelScope.launch {
            val book = _uiState.value.selectedBook
            val newGoal = Goal(
                bookTitle = book?.title ?: _uiState.value.bookTitleInput,
                bookIsbn = book?.isbn13 ?: "",
                bookCover = book?.cover ?: "",
                duration = _uiState.value.durationInput,
                pages = _uiState.value.pagesInput
            )

            _uiState.update { currentState ->
                currentState.copy(goals = currentState.goals + newGoal)
            }
            onShowGoalInputs(false)

            val success = firestoreRepository.saveGoal(newGoal)
            if (!success) {
                _uiState.update { currentState ->
                    currentState.copy(
                        goals = currentState.goals.filterNot { it == newGoal },
                        errorMessage = "목표 저장에 실패했습니다."
                    )
                }
            }
        }
    }
}
