package com.route.readers.ui.screens.profile

import com.route.readers.data.model.Book
import com.route.readers.data.model.User

sealed class ProfileUiState {
    object Loading : ProfileUiState()

    data class Success(
        val user: User,
        val isFollowing: Boolean,
        val isMyProfile: Boolean,
        val recommendedBooks: List<Book>,
        val favoriteBooks: List<Book>
    ) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}
