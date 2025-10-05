package com.route.readers.ui.screens.profile

import com.route.readers.data.model.Book
import com.route.readers.data.model.User

/**
 * 프로필 화면의 UI 상태를 나타내는 Sealed Class
 */
sealed class ProfileUiState {
    // 로딩 중 상태
    object Loading : ProfileUiState()

    // 데이터 로드 성공 상태
    data class Success(
        val user: User,
        val isFollowing: Boolean,
        val isMyProfile: Boolean,
        val recommendedBooks: List<Book>
    ) : ProfileUiState()

    // 오류 발생 상태
    data class Error(val message: String) : ProfileUiState()
}
