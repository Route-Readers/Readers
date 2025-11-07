package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.data.remote.MyLibraryRepository
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
    val selectedMyBook: MyBook? = null,
    val durationInput: String = "",
    val pagesInput: String = "",
    val showGoalInputs: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val myBooks: List<MyBook> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class GoalViewModel : ViewModel() {

    private val firestoreRepository = FirestoreRepository()
    private val myLibraryRepository = MyLibraryRepository()

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val goals = firestoreRepository.getGoals()
            val myBooks = myLibraryRepository.getMyBooks()
            _uiState.update {
                it.copy(
                    goals = goals,
                    myBooks = myBooks.filter { book -> !book.isCompleted },
                    isLoading = false
                )
            }
        }
    }

    fun onMyBookSelected(book: MyBook) {
        _uiState.update {
            it.copy(
                selectedMyBook = book,
                pagesInput = if (book.totalPages > 0) book.totalPages.toString() else ""
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
                    selectedMyBook = null,
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
            val book = _uiState.value.selectedMyBook ?: return@launch

            val newGoal = Goal(
                bookTitle = book.title,
                bookIsbn = book.isbn,
                bookCover = book.cover,
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
