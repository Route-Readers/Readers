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

// Goal 데이터 클래스에 currentPage 추가
data class Goal(
    val bookTitle: String = "",
    val bookIsbn: String = "",
    val bookCover: String = "",
    val duration: String = "",
    val pages: String = "",
    val dailyPages: Int = 0,
    val currentPage: Int = 0 // 현재 읽은 페이지
)

data class GoalUiState(
    val selectedMyBook: MyBook? = null,
    val durationInput: String = "",
    val pagesInput: String = "",
    val dailyPages: Int = 0,
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
            val goalsFromRepo = firestoreRepository.getGoals()
            val myBooks = myLibraryRepository.getMyBooks()

            // 목표 목록에 현재 읽은 페이지(currentPage) 정보를 업데이트
            val updatedGoals = goalsFromRepo.map { goal ->
                val correspondingBook = myBooks.find { it.isbn == goal.bookIsbn }
                goal.copy(currentPage = correspondingBook?.currentPage ?: 0)
            }

            _uiState.update {
                it.copy(
                    goals = updatedGoals,
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
                    dailyPages = 0
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
                dailyPages = currentState.dailyPages,
                currentPage = book.currentPage // 저장 시점의 현재 페이지 저장
            )

            _uiState.update {
                it.copy(goals = it.goals + newGoal)
            }
            onShowGoalInputs(false)

            // Firestore에 Goal 객체를 저장할 때 currentPage는 제외하고 저장하거나,
            // 혹은 저장하되 앱 실행 시 항상 MyBook 데이터 기준으로 덮어쓰도록 합니다.
            // 여기서는 Firestore에 저장하는 Goal 객체에서는 currentPage를 제외하는 것을 권장합니다.
            // 아래는 Firestore 저장용 객체에서 currentPage를 빼는 예시입니다.
            val goalForFirestore = newGoal.copy(currentPage = 0) // Firestore에는 진행률을 저장하지 않음
            val success = firestoreRepository.saveGoal(goalForFirestore)

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
