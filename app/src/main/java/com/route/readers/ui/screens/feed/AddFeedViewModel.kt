package com.route.readers.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AddFeedUiState {
    data object Idle : AddFeedUiState()
    data object Loading : AddFeedUiState()
    data object Success : AddFeedUiState()
    data class Error(val message: String) : AddFeedUiState()
}

class AddFeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<AddFeedUiState>(AddFeedUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun addFeed(bookTitle: String, review: String, rating: Int) {
        viewModelScope.launch {
            _uiState.value = AddFeedUiState.Loading
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _uiState.value = AddFeedUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            if (bookTitle.isBlank() || review.isBlank()) {
                _uiState.value = AddFeedUiState.Error("책 이름과 후기를 모두 입력해주세요.")
                return@launch
            }

            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val user = userDoc.toObject(User::class.java)
                if (user == null) {
                    _uiState.value = AddFeedUiState.Error("사용자 정보를 가져올 수 없습니다.")
                    return@launch
                }

                val feedDocRef = db.collection("feeds").document()

                val newFeed = FeedItem.BookReview(
                    id = feedDocRef.id,
                    authorId = currentUserId,
                    userName = user.nickname,
                    bookTitle = bookTitle,
                    review = review,
                    rating = rating,
                    likedBy = emptyList(),
                    // ▼▼▼ 여기가 수정된 부분입니다 ▼▼▼
                    timestamp = Timestamp.now(),
                    likeCount = 0,
                    commentCount = 0
                )

                feedDocRef.set(newFeed).await()

                _uiState.value = AddFeedUiState.Success
            } catch (e: Exception) {
                _uiState.value = AddFeedUiState.Error("피드 저장 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }
}
