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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class FeedUiState {
    data object Loading : FeedUiState()
    data class Success(
        val items: List<FeedItem>,
        val likedFeedIds: Set<String>,
        val savedFeedIds: Set<String>,
        val followerInfoMap: Map<String, User> = emptyMap()
    ) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        loadFeeds(isRefresh = false)
    }

    fun refreshFeeds() {
        loadFeeds(isRefresh = true)
    }

    private fun loadFeeds(isRefresh: Boolean) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _uiState.value = FeedUiState.Error("로그인이 필요합니다.")
            return
        }

        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = FeedUiState.Loading
            }

            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val user = userDoc.toObject(User::class.java)
                val followingList = user?.following ?: emptyList()
                val blockedUserList = user?.blockedUsers ?: emptyList()
                val feedAuthors = (followingList + currentUserId).distinct()

                if (feedAuthors.isEmpty()) {
                    _uiState.value = FeedUiState.Success(emptyList(), emptySet(), emptySet(), emptyMap())
                    if (isRefresh) _isRefreshing.value = false
                    return@launch
                }

                val savedFeedIdsFromUser = user?.savedFeeds?.toSet() ?: emptySet()

                val querySnapshot = db.collection("feeds")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val feeds = querySnapshot.documents.mapNotNull { doc ->
                    doc.toFeedItem()
                }.filter { feed ->
                    when (feed) {
                        is FeedItem.BookReview -> feed.authorId in feedAuthors && feed.authorId !in blockedUserList
                        is FeedItem.FollowNotification -> feed.receiverId == currentUserId && feed.followerId !in blockedUserList
                    }
                }

                val followNotifications = feeds.filterIsInstance<FeedItem.FollowNotification>()
                val followerIds = followNotifications.map { it.followerId }.distinct()

                val followerInfoMap = if (followerIds.isNotEmpty()) {
                    val followerDocs = db.collection("users")
                        .whereIn("uid", followerIds)
                        .get()
                        .await()
                    followerDocs.associate { doc ->
                        val followerUser = doc.toObject(User::class.java)
                        followerUser.uid to followerUser
                    }
                } else {
                    emptyMap()
                }

                val updatedFeeds = feeds.map { feed ->
                    if (feed is FeedItem.FollowNotification) {
                        val followerName = followerInfoMap[feed.followerId]?.nickname ?: feed.userName
                        feed.copy(userName = followerName)
                    } else {
                        feed
                    }
                }

                val likedFeedIds = updatedFeeds
                    .filterIsInstance<FeedItem.BookReview>()
                    .filter { it.likedBy.contains(currentUserId) }
                    .map { it.id }
                    .toSet()

                _uiState.value = FeedUiState.Success(updatedFeeds, likedFeedIds, savedFeedIdsFromUser, followerInfoMap)

            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error loading feeds", e)
                _uiState.value = FeedUiState.Error("피드 로딩 실패: ${e.message}")
            } finally {
                if (isRefresh) {
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun toggleLike(feedId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return

        _uiState.update { currentState ->
            if (currentState is FeedUiState.Success) {
                val updatedItems = currentState.items.map { item ->
                    if (item.id == feedId && item is FeedItem.BookReview) {
                        val newLikeCount = if (isCurrentlyLiked) (item.likeCount - 1).coerceAtLeast(0) else item.likeCount + 1
                        item.copy(
                            likeCount = newLikeCount,
                            likedBy = if (isCurrentlyLiked) item.likedBy - currentUserId else item.likedBy + currentUserId
                        )
                    } else {
                        item
                    }
                }
                currentState.copy(
                    items = updatedItems,
                    likedFeedIds = if (isCurrentlyLiked) currentState.likedFeedIds - feedId else currentState.likedFeedIds + feedId
                )
            } else {
                currentState
            }
        }

        viewModelScope.launch {
            try {
                val feedRef = db.collection("feeds").document(feedId)
                val operation = if (isCurrentlyLiked) {
                    FieldValue.arrayRemove(currentUserId)
                } else {
                    FieldValue.arrayUnion(currentUserId)
                }
                feedRef.update("likedBy", operation).await()

                val document = feedRef.get().await()
                val likedByList = document.get("likedBy") as? List<*>
                val actualLikeCount = likedByList?.size ?: 0
                feedRef.update("likeCount", actualLikeCount).await()

            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error toggling like for feed $feedId. Reverting UI.", e)
                loadFeeds(isRefresh = false)
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

    fun followBack(followerId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val currentUserRef = db.collection("users").document(currentUserId)
                currentUserRef.update("following", FieldValue.arrayUnion(followerId)).await()

                val followerRef = db.collection("users").document(followerId)
                followerRef.update("followers", FieldValue.arrayUnion(currentUserId)).await()

                loadFeeds(isRefresh = false)
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error following back user $followerId", e)
            }
        }
    }
}
