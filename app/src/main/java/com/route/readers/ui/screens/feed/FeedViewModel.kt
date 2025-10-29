package com.route.readers.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.data.model.User
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.data.remote.WishlistRepository
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
        val bookmarkedFeedIds: Set<String>,
        val followerInfoMap: Map<String, User> = emptyMap(),
        val wishlist: List<String> = emptyList(),
        val myLibrary: List<String> = emptyList()
    ) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val wishlistRepository = WishlistRepository()
    private val myLibraryRepository = MyLibraryRepository()

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

    fun refreshUserProfiles() {
        Log.d("FeedViewModel", "refreshUserProfiles called")
        val currentState = _uiState.value
        if (currentState is FeedUiState.Success) {
            Log.d("FeedViewModel", "Current state is Success, items count: ${currentState.items.size}")
            viewModelScope.launch {
                try {
                    val updatedFollowerInfoMap = mutableMapOf<String, User>()
                    
                    // 현재 피드에 있는 모든 사용자 ID 수집
                    val userIds = mutableSetOf<String>()
                    currentState.items.forEach { item ->
                        when (item) {
                            is FeedItem.BookReview -> {
                                userIds.add(item.authorId)
                                Log.d("FeedViewModel", "Added BookReview authorId: ${item.authorId}")
                            }
                            is FeedItem.FollowNotification -> {
                                userIds.add(item.authorId)
                                userIds.add(item.followerId)
                                Log.d("FeedViewModel", "Added FollowNotification authorId: ${item.authorId}, followerId: ${item.followerId}")
                            }
                        }
                    }
                    
                    Log.d("FeedViewModel", "Total userIds to refresh: ${userIds.size}")
                    
                    // 각 사용자의 최신 정보 가져오기
                    for (userId in userIds) {
                        try {
                            val userDoc = db.collection("users").document(userId).get().await()
                            if (userDoc.exists()) {
                                val user = userDoc.toObject(User::class.java)
                                if (user != null) {
                                    updatedFollowerInfoMap[userId] = user
                                    Log.d("FeedViewModel", "Updated user $userId: character=${user.profileCharacter}, bgColor=${user.profileBackgroundColor}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FeedViewModel", "Error fetching user $userId", e)
                        }
                    }
                    
                    Log.d("FeedViewModel", "Updating UI state with ${updatedFollowerInfoMap.size} users")
                    // UI 상태 업데이트
                    _uiState.value = currentState.copy(followerInfoMap = updatedFollowerInfoMap)
                } catch (e: Exception) {
                    Log.e("FeedViewModel", "Error refreshing user profiles", e)
                }
            }
        } else {
            Log.d("FeedViewModel", "Current state is not Success: ${currentState::class.simpleName}")
        }
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

                val allUserIds = mutableSetOf<String>()
                feeds.forEach { feed ->
                    when (feed) {
                        is FeedItem.BookReview -> allUserIds.add(feed.authorId)
                        is FeedItem.FollowNotification -> {
                            allUserIds.add(feed.authorId)
                            allUserIds.add(feed.followerId)
                        }
                    }
                }
                
                val followerInfoMap = if (allUserIds.isNotEmpty()) {
                    val userInfoMap = mutableMapOf<String, User>()
                    allUserIds.chunked(10).forEach { userIdChunk ->
                        try {
                            val userDocs = db.collection("users")
                                .whereIn("uid", userIdChunk)
                                .get()
                                .await()
                            userDocs.forEach { doc ->
                                val user = doc.toObject(User::class.java)
                                userInfoMap[user.uid] = user
                            }
                        } catch (e: Exception) {
                            Log.e("FeedViewModel", "Error loading user info for chunk: $userIdChunk", e)
                        }
                    }
                    userInfoMap
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

                val bookmarkedFeedIds = updatedFeeds
                    .filterIsInstance<FeedItem.BookReview>()
                    .filter { it.bookmarkedBy.contains(currentUserId) }
                    .map { it.id }
                    .toSet()

                val wishlist = wishlistRepository.getWishlist()
                val myLibrary = myLibraryRepository.getMyBooks().map { it.isbn }

                _uiState.value = FeedUiState.Success(updatedFeeds, likedFeedIds, bookmarkedFeedIds, followerInfoMap, wishlist, myLibrary)

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

    fun toggleBookmark(feedId: String, isCurrentlyBookmarked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return

        _uiState.update { currentState ->
            if (currentState is FeedUiState.Success) {
                val updatedItems = currentState.items.map { item ->
                    if (item.id == feedId && item is FeedItem.BookReview) {
                        item.copy(
                            isBookmarked = !isCurrentlyBookmarked,
                            bookmarkedBy = if (isCurrentlyBookmarked) item.bookmarkedBy - currentUserId else item.bookmarkedBy + currentUserId
                        )
                    } else {
                        item
                    }
                }
                currentState.copy(
                    items = updatedItems,
                    bookmarkedFeedIds = if (isCurrentlyBookmarked) currentState.bookmarkedFeedIds - feedId else currentState.bookmarkedFeedIds + feedId
                )
            } else {
                currentState
            }
        }

        viewModelScope.launch {
            try {
                val feedRef = db.collection("feeds").document(feedId)
                val operation = if (isCurrentlyBookmarked) {
                    FieldValue.arrayRemove(currentUserId)
                } else {
                    FieldValue.arrayUnion(currentUserId)
                }
                feedRef.update("bookmarkedBy", operation).await()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error toggling bookmark for feed $feedId. Reverting UI.", e)
                loadFeeds(isRefresh = false) // Revert UI on error
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

    fun toggleWishlist(book: Book, isInWishlist: Boolean) {
        viewModelScope.launch {
            val success = if (isInWishlist) {
                wishlistRepository.removeFromWishlist(book.isbn)
            } else {
                wishlistRepository.addToWishlist(book)
            }
            if (success) {
                val currentState = _uiState.value
                if (currentState is FeedUiState.Success) {
                    val updatedWishlist = if (isInWishlist) {
                        currentState.wishlist - book.isbn
                    } else {
                        currentState.wishlist + book.isbn
                    }
                    _uiState.value = currentState.copy(wishlist = updatedWishlist)
                }
            }
        }
    }

    fun toggleMyLibrary(book: Book, isInMyLibrary: Boolean) {
        viewModelScope.launch {
            val success = if (isInMyLibrary) {
                myLibraryRepository.removeBookFromLibrary(book.isbn)
            } else {
                myLibraryRepository.addBookToLibrary(MyBook(
                    id = book.isbn, 
                    title = book.title, 
                    author = book.author, 
                    cover = book.cover, 
                    isbn = book.isbn,
                    totalPages = 0,
                    currentPage = 0,
                    isCompleted = false,
                    addedDate = System.currentTimeMillis(),
                    lastReadDate = System.currentTimeMillis(),
                    completedDate = null
                    ))
            }
            if (success) {
                val currentState = _uiState.value
                if (currentState is FeedUiState.Success) {
                    val updatedMyLibrary = if (isInMyLibrary) {
                        currentState.myLibrary - book.isbn
                    } else {
                        currentState.myLibrary + book.isbn
                    }
                    _uiState.value = currentState.copy(myLibrary = updatedMyLibrary)
                }
            }
        }
    }
}
