package com.route.readers.ui.screens.bookclub

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.route.readers.data.model.BookClub

class BookClubViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(BookClubUiState())
    val uiState: StateFlow<BookClubUiState> = _uiState.asStateFlow()
    
    init {
        loadBookClubs()
    }
    
    private fun loadBookClubs() {
        // 임시 데이터
        val sampleClubs = listOf(
            BookClub(
                id = "1",
                name = "소설 읽기 모임",
                bookTitle = "미드나잇 라이브러리",
                memberCount = 12,
                isJoined = false
            ),
            BookClub(
                id = "2", 
                name = "자기계발서 클럽",
                bookTitle = "아토믹 해빗",
                memberCount = 8,
                isJoined = true
            )
        )
        
        _uiState.value = _uiState.value.copy(bookClubs = sampleClubs)
    }
    
    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }
    
    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }
    
    fun createBookClub(name: String, bookTitle: String) {
        val newClub = BookClub(
            id = System.currentTimeMillis().toString(),
            name = name,
            bookTitle = bookTitle,
            memberCount = 1,
            isJoined = true
        )
        
        val updatedClubs = _uiState.value.bookClubs + newClub
        _uiState.value = _uiState.value.copy(
            bookClubs = updatedClubs,
            showCreateDialog = false
        )
    }
    
    fun joinBookClub(clubId: String) {
        val updatedClubs = _uiState.value.bookClubs.map { club ->
            if (club.id == clubId) {
                club.copy(
                    isJoined = !club.isJoined,
                    memberCount = if (club.isJoined) club.memberCount - 1 else club.memberCount + 1
                )
            } else club
        }
        
        _uiState.value = _uiState.value.copy(bookClubs = updatedClubs)
    }
}

data class BookClubUiState(
    val bookClubs: List<BookClub> = emptyList(),
    val showCreateDialog: Boolean = false
)
