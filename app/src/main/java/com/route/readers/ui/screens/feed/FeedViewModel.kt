package com.route.readers.ui.screens.feed

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val items: List<FeedItem>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadMockFeedData()
    }

    private fun loadMockFeedData() {
        val now = Timestamp(Date())
        val mockItems = listOf(
            FeedItem.Follow(
                follower = "김독서",
                following = "김이삭",
                timestamp = now
            ),
            FeedItem.ChallengeStart(
                users = "김이삭님과 김독서님",
                description = "친구와 10일동안 100페이지 읽기 챌린지를 시작하셨어요!",
                timestamp = now
            ),
            FeedItem.ReadingProgress(
                user = "김독서",
                bookTitle = "데미안",
                currentPage = 45,
                totalPages = 200,
                description = "오늘 30분 독서했어요",
                timestamp = now
            ),
            FeedItem.ChallengeSuccess(
                users = "김이삭님과 김독서님",
                description = "1주일동안 매일 책읽기 챌린지를 성공하셨습니다!",
                timestamp = now
            ),
            FeedItem.BookReview(
                user = "이책좋아",
                bookTitle = "해리포터와 마법사의 돌",
                rating = 5,
                review = "정말 재미있었어요! 마법의 세계가 생생했습니다.",
                timestamp = now
            )
        )
        _uiState.value = FeedUiState.Success(mockItems)
    }
}
