package com.route.readers.ui.screens.profile

import com.route.readers.data.model.Book
// import com.route.readers.data.model.Challenge // 더 이상 직접 사용하지 않으므로 삭제 가능
import com.route.readers.data.model.User
import com.route.readers.ui.screens.feed.FeedItem

sealed class ProfileUiState {
    data object Loading : ProfileUiState()

    data class Success(
        val user: User,
        val isFollowing: Boolean,
        val isMyProfile: Boolean,
        val isBlocked: Boolean,
        val recommendedBooks: List<Book>,
        val favoriteBooks: List<Book>,
        val achievements: List<Achievement>,
        val myPosts: List<FeedItem>,
        val savedPosts: List<FeedItem>,
        val isSelectionMode: Boolean = false,
        val selectedBookIds: Set<String> = emptySet(),
        val likedFeedIds: Set<String> = emptySet(),
        val bookmarkedFeedIds: Set<String> = emptySet(),
        val wishlist: List<String> = emptyList(),
        val myLibrary: List<String> = emptyList(),
        val userInfoMap: Map<String, User> = emptyMap()
    ) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}
