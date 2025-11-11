package com.route.readers.ui.screens.bookclub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.BookClub
import com.route.readers.data.remote.BookClubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookClubViewModel(
    private val repository: BookClubRepository = BookClubRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookClubUiState())
    val uiState: StateFlow<BookClubUiState> = _uiState.asStateFlow()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        loadBookClubsRealtime()
    }

    private fun loadBookClubsRealtime() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                repository.getAllBookClubsFlow().collect { bookClubs ->
                    val updatedClubs = bookClubs.map { club ->
                        club.copy(isJoined = club.members.contains(currentUserId))
                    }
                    _uiState.value = _uiState.value.copy(
                        bookClubs = updatedClubs,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "알 수 없는 오류가 발생했습니다."
                )
            }
        }
    }

    fun loadBookClubs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.getAllBookClubs().fold(
                onSuccess = { bookClubs ->
                    val updatedClubs = bookClubs.map { club ->
                        club.copy(isJoined = club.members.contains(currentUserId))
                    }
                    _uiState.value = _uiState.value.copy(
                        bookClubs = updatedClubs,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "북클럽을 불러오는데 실패했습니다."
                    )
                }
            )
        }
    }

    fun createBookClub(name: String, description: String, bookTitle: String) {
        if (name.isBlank() || bookTitle.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "북클럽 이름과 책 제목을 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            
            val bookClub = BookClub(
                name = name.trim(),
                description = description.trim(),
                currentBook = bookTitle.trim(),
                bookTitle = bookTitle.trim(),
                createdBy = currentUserId,
                members = listOf(currentUserId),
                memberCount = 1,
                createdAt = System.currentTimeMillis()
            )

            repository.createBookClub(bookClub).fold(
                onSuccess = {
                    hideCreateDialog()
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = error.message ?: "북클럽 생성에 실패했습니다."
                    )
                }
            )
        }
    }

    fun joinBookClub(bookClubId: String) {
        viewModelScope.launch {
            repository.joinBookClub(bookClubId, currentUserId).fold(
                onSuccess = { 
                    _uiState.value = _uiState.value.copy(error = null)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "북클럽 참여에 실패했습니다."
                    )
                }
            )
        }
    }

    fun leaveBookClub(bookClubId: String) {
        viewModelScope.launch {
            repository.leaveBookClub(bookClubId, currentUserId).fold(
                onSuccess = { 
                    _uiState.value = _uiState.value.copy(error = null)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "북클럽 탈퇴에 실패했습니다."
                    )
                }
            )
        }
    }

    fun deleteBookClub(bookClubId: String) {
        viewModelScope.launch {
            repository.deleteBookClub(bookClubId, currentUserId).fold(
                onSuccess = { 
                    _uiState.value = _uiState.value.copy(error = null)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "북클럽 삭제에 실패했습니다."
                    )
                }
            )
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = false,
            isCreating = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class BookClubUiState(
    val bookClubs: List<BookClub> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val showCreateDialog: Boolean = false,
    val error: String? = null
)
