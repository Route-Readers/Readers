package com.route.readers.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class FeedUiState {
    data object Loading : FeedUiState()
    data class Success(
        val items: List<FeedItem>,
        val likedFeedIds: Set<String>,
        val savedFeedIds: Set<String>
    ) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadFeeds()
    }

    private fun loadFeeds() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _uiState.value = FeedUiState.Error("로그인이 필요합니다.")
            return
        }

        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val followingList = userDoc.toObject(User::class.java)?.following ?: emptyList()
                val feedAuthors = (followingList + currentUserId).distinct()

                if (feedAuthors.isEmpty()) {
                    _uiState.value = FeedUiState.Success(emptyList(), emptySet(), emptySet())
                    return@launch
                }

                val savedFeedIdsFromUser = userDoc.toObject(User::class.java)?.savedFeeds?.toSet() ?: emptySet()

                db.collection("feeds")
                    .whereIn("authorId", feedAuthors.take(30))
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(20)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            val errorMessage = e.message ?: "알 수 없는 오류"
                            if (errorMessage.contains("FAILED_PRECONDITION")) {
                                _uiState.value = FeedUiState.Error("피드를 불러오는데 필요한 데이터베이스 색인이 없습니다. Firebase 콘솔에서 색인을 생성해주세요.")
                            } else {
                                _uiState.value = FeedUiState.Error("피드 로딩 실패: $errorMessage")
                            }
                            Log.e("FeedViewModel", "Snapshot listener error", e)
                            return@addSnapshotListener
                        }

                        if (snapshots != null) {
                            val feeds = snapshots.mapNotNull { doc ->
                                when (doc.getString("type")) {
                                    "BOOK_REVIEW" -> doc.toObject(FeedItem.BookReview::class.java)
                                    else -> null
                                }
                            }
                            val likedFeedIds = feeds
                                .filterIsInstance<FeedItem.BookReview>()
                                .filter { it.likedBy.contains(currentUserId) }
                                .map { it.id }
                                .toSet()
                            _uiState.value = FeedUiState.Success(feeds, likedFeedIds, savedFeedIdsFromUser)
                        }
                    }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error loading initial user data", e)
                _uiState.value = FeedUiState.Error("피드 로딩 실패: ${e.message}")
            }
        }
    }

    fun toggleLike(feedId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val feedRef = db.collection("feeds").document(feedId)
            try {
                if (isCurrentlyLiked) {
                    feedRef.update(
                        "likedBy", FieldValue.arrayRemove(currentUserId),
                        "likeCount", FieldValue.increment(-1)
                    )
                } else {
                    feedRef.update(
                        "likedBy", FieldValue.arrayUnion(currentUserId),
                        "likeCount", FieldValue.increment(1)
                    )
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error toggling like for feed $feedId", e)
            }
        }
    }

    fun toggleSave(feedId: String, isCurrentlySaved: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val userRef = db.collection("users").document(currentUserId)
            try {
                val operation = if (isCurrentlySaved) {
                    FieldValue.arrayRemove(feedId)
                } else {
                    FieldValue.arrayUnion(feedId)
                }
                userRef.update("savedFeeds", operation).await()

                val currentState = _uiState.value
                if (currentState is FeedUiState.Success) {
                    val newSavedIds = if (isCurrentlySaved) {
                        currentState.savedFeedIds - feedId
                    } else {
                        currentState.savedFeedIds + feedId
                    }
                    _uiState.value = currentState.copy(savedFeedIds = newSavedIds)
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error toggling save for feed $feedId", e)
            }
        }
    }

    fun deleteFeed(feedId: String) {
        viewModelScope.launch {
            try {
                db.collection("feeds").document(feedId).delete().await()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error deleting feed $feedId", e)
            }
        }
    }
}
