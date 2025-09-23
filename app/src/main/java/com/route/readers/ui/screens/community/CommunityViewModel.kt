package com.route.readers.ui.screens.community

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Friend(
    val name: String,
    val currentBook: String,
    val isOnline: Boolean,
    val lastActive: String
)

data class CommunityUiState(
    val friends: List<Friend> = emptyList(),
    val challengeProgress: Float = 0.67f,
    val challengeParticipants: Int = 156
)

class CommunityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        CommunityUiState(
            friends = listOf(
                Friend("김지연", "최근 본 책: 아토믹 해빗", true, "온라인"),
                Friend("박민수", "최근 본 책: 사피엔스", false, "30분 전"),
                Friend("이서현", "최근 본 책: 해리 포터와 마법사의 돌", false, "1시간 전")
            )
        )
    )
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
}
