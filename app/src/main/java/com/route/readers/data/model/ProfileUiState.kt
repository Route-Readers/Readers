package com.route.readers.data.model


sealed class ProfileUiState {

    object Loading : ProfileUiState()

    data class Success(val user: User,
                       val isFollowing: Boolean,
                       val isMyProfile: Boolean
    ) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}
