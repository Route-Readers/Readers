package com.route.readers.ui.screens.profile

import com.route.readers.data.model.Book
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.User

sealed class ProfileUiState {
    data object Loading : ProfileUiState()

    data class Success(
        val user: User,
        val isFollowing: Boolean,
        val isMyProfile: Boolean,
        val recommendedBooks: List<Book>,
        val favoriteBooks: List<Book>,
        val ongoingChallenges: List<Challenge>,
        val completedChallenges: List<Challenge>,
        val isSelectionMode: Boolean = false,
        val selectedBookIds: Set<String> = emptySet()
    ) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}
