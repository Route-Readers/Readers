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
import kotlin.math.ceil

data class Goal(
    val bookTitle: String = "",
    val bookIsbn: String = "",
    val bookCover: String = "",
    val duration: String = "",
    val pages: String = "",
    val dailyPages: Int = 0 // 일일 목표 페이지 추가
)

data class GoalUiState(
    val selectedMyBook: MyBook? = null,
    val durationInput: String = "",
    val pagesInput: String = "",
    val dailyPages: Int = 0, // 계산된 일일 목표 페이지 추가
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
        // 책이 선택될 때도 일일 페이지 수를 다시 계산합니다.
        calculateDailyPages(_uiState.value.durationInput, _uiState.value.pagesInput)
    }

    fun onDurationChange(newDuration: String) {
        _uiState.update { it.copy(durationInput = newDuration) }
        calculateDailyPages(newDuration, _uiState.value.pagesInput)
    }

    fun onPagesChange(newPages: String) {
        _uiState.update { it.copy(pagesInput = newPages) }
        calculateDailyPages(_uiState.value.durationInput, newPages)
    }

    private fun calculateDailyPages(durationStr: String, pagesStr: String) {
        val duration = durationStr.filter { it.isDigit() }.toIntOrNull()
        val pages = pagesStr.toIntOrNull()

        if (duration != null && pages != null && duration > 0 && pages > 0) {
            val dailyPages = ceil(pages.toDouble() / duration.toDouble()).toInt()
            _uiState.update { it.copy(dailyPages = dailyPages) }
        } else {
            _uiState.update { it.copy(dailyPages = 0) }
        }
    }

    fun onShowGoalInputs(show: Boolean) {
        if (!show) {
            _uiState.update {
                it.copy(
                    showGoalInputs = false,
                    selectedMyBook = null,
                    durationInput = "",
                    pagesInput = "",
                    dailyPages = 0 // 입력창을 닫을 때 초기화
                )
            }
        } else {
            _uiState.update { it.copy(showGoalInputs = true) }
        }
    }

    fun saveGoal() {
        viewModelScope.launch {
            val book = _uiState.value.selectedMyBook ?: return@launch
            val currentState = _uiState.value

            val newGoal = Goal(
                bookTitle = book.title,
                bookIsbn = book.isbn,
                bookCover = book.cover,
                duration = currentState.durationInput,
                pages = currentState.pagesInput,
                dailyPages = currentState.dailyPages // 계산된 일일 목표 페이지 저장
            )

            _uiState.update {
                it.copy(goals = it.goals + newGoal)
            }
            onShowGoalInputs(false)

            val success = firestoreRepository.saveGoal(newGoal)
            if (!success) {
                _uiState.update {
                    it.copy(
                        goals = it.goals.filterNot { goal -> goal == newGoal },
                        errorMessage = "목표 저장에 실패했습니다."
                    )
                }
            }
        }
    }
}
