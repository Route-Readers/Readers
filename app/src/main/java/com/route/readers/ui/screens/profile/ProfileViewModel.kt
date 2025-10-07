package com.route.readers.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.Book
import com.route.readers.data.model.User
import com.route.readers.data.remote.BookRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


sealed class ProfileSetupState {
    object Idle : ProfileSetupState()
    object Loading : ProfileSetupState()
    object Success : ProfileSetupState()
    data class Error(val message: String) : ProfileSetupState()
}

open class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookRepository = BookRepository()
    private val currentUserId = auth.currentUser?.uid

    protected val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _setupState = MutableStateFlow<ProfileSetupState>(ProfileSetupState.Idle)
    val setupState: StateFlow<ProfileSetupState> = _setupState

    fun checkNicknameAvailability(nickname: String) {
        if (nickname.length !in 2..12) {
            _setupState.value = ProfileSetupState.Error("닉네임은 2~12자 사이로 입력해주세요.")
            return
        }
        _setupState.value = ProfileSetupState.Loading
        viewModelScope.launch {
            try {
                val documents =
                    db.collection("users").whereEqualTo("nickname", nickname).get().await()
                if (documents.isEmpty) {
                    _setupState.value = ProfileSetupState.Error("사용 가능한 닉네임입니다.")
                } else {
                    _setupState.value = ProfileSetupState.Error("이미 사용 중인 닉네임입니다.")
                }
            } catch (e: Exception) {
                _setupState.value = ProfileSetupState.Error("오류 발생: ${e.message}")
            }
        }
    }

    fun createOrUpdateUserProfile(
        nickname: String,
        genres: List<String>,
        styles: List<String>
    ) {
        if (currentUserId == null) {
            _setupState.value = ProfileSetupState.Error("사용자 인증 정보가 없습니다. 다시 로그인해주세요.")
            return
        }

        _setupState.value = ProfileSetupState.Loading

        val userProfileData = mapOf(
            "uid" to currentUserId,
            "nickname" to nickname,
            "email" to auth.currentUser?.email,
            "readingGenres" to genres,
            "readingStyles" to styles,
            "level" to 1,
            "followerCount" to 0,
            "followingCount" to 0,
            "readBookCount" to 0,
            "followers" to emptyList<String>(),
            "following" to emptyList<String>(),
            "isCurrentlyReading" to false,
            "profileImageUrl" to null
        )

        viewModelScope.launch {
            try {
                db.collection("users").document(currentUserId)
                    .set(userProfileData)
                    .await()
                _setupState.value = ProfileSetupState.Success
            } catch (e: Exception) {
                _setupState.value = ProfileSetupState.Error("프로필 저장에 실패했습니다: ${e.message}")
            }
        }
    }

    open fun fetchUserProfile(userId: String?) {
        val targetUserId = userId ?: currentUserId

        if (targetUserId == null) {
            _uiState.value = ProfileUiState.Error("사용자 정보를 찾을 수 없습니다.")
            return
        }

        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                val userDocumentDeferred =
                    async { db.collection("users").document(targetUserId).get().await() }
                val favoriteBooksDeferred = async { bookRepository.getFavoriteBooks() }

                val document = userDocumentDeferred.await()
                val favoriteBooks = favoriteBooksDeferred.await()

                if (document.exists()) {
                    val user: User? = document.toObject(User::class.java)
                    if (user != null) {
                        val isMyProfile = targetUserId == currentUserId
                        val isFollowing = user.followers.contains(currentUserId)
                        val recommendedBooks = fetchRecommendedBooks(user.readingGenres)

                        _uiState.value = ProfileUiState.Success(
                            user = user,
                            isFollowing = isFollowing,
                            isMyProfile = isMyProfile,
                            recommendedBooks = recommendedBooks,
                            favoriteBooks = favoriteBooks
                        )
                    } else {
                        _uiState.value = ProfileUiState.Error("프로필 정보를 변환하는 데 실패했습니다.")
                    }
                } else {
                    _uiState.value = ProfileUiState.Error("프로필 데이터가 존재하지 않습니다.")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("프로필을 불러오는 중 오류가 발생했습니다: ${e.message}")
                Log.e("ProfileViewModel", "fetchUserProfile failed", e)
            }
        }
    }

    private suspend fun fetchRecommendedBooks(genres: List<String>): List<Book> {
        if (genres.isEmpty()) {
            return emptyList()
        }

        return try {
            coroutineScope {
                val deferredBookLists = genres.map { genre ->
                    async {
                        bookRepository.getBookSearch(query = genre, maxResults = 3)
                    }
                }
                deferredBookLists.awaitAll()
                    .flatMap { it }
                    .distinctBy { it.isbn }
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "추천 도서 API 호출 실패", e)
            emptyList()
        }
    }

    fun followUser(targetUserId: String) {
        if (currentUserId == null) return
        refreshUiStateForFollow(targetUserId, true)
        viewModelScope.launch {
            try {
                val targetUserRef = db.collection("users").document(targetUserId)
                val currentUserRef = db.collection("users").document(currentUserId)
                db.runBatch { batch ->
                    batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))
                    batch.update(targetUserRef, "followerCount", FieldValue.increment(1))
                    batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))
                    batch.update(currentUserRef, "followingCount", FieldValue.increment(1))
                }.await()
            } catch (e: Exception) {
                refreshUiStateForFollow(targetUserId, false)
            }
        }
    }

    fun unfollowUser(targetUserId: String) {
        if (currentUserId == null) return
        refreshUiStateForFollow(targetUserId, false)
        viewModelScope.launch {
            try {
                val targetUserRef = db.collection("users").document(targetUserId)
                val currentUserRef = db.collection("users").document(currentUserId)
                db.runBatch { batch ->
                    batch.update(
                        targetUserRef,
                        "followers",
                        FieldValue.arrayRemove(currentUserId)
                    )
                    batch.update(targetUserRef, "followerCount", FieldValue.increment(-1))
                    batch.update(
                        currentUserRef,
                        "following",
                        FieldValue.arrayRemove(targetUserId)
                    )
                    batch.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                }.await()
            } catch (e: Exception) {
                refreshUiStateForFollow(targetUserId, true)
            }
        }
    }

    private fun refreshUiStateForFollow(targetUserId: String, nowFollowing: Boolean) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            val user = currentState.user

            if (!currentState.isMyProfile) {
                val newFollowerCount = if (nowFollowing) {
                    user.followerCount + 1
                } else {
                    (user.followerCount - 1).coerceAtLeast(0)
                }
                _uiState.value = currentState.copy(
                    isFollowing = nowFollowing,
                    user = user.copy(followerCount = newFollowerCount)
                )
            } else {
                val newFollowingCount = if (nowFollowing) {
                    user.followingCount + 1
                } else {
                    (user.followingCount - 1).coerceAtLeast(0)
                }
                _uiState.value = currentState.copy(
                    user = user.copy(followingCount = newFollowingCount)
                )
            }
        } else {
            fetchUserProfile(targetUserId)
        }
    }

    fun toggleBookSelection(bookId: String) {
        (_uiState.value as? ProfileUiState.Success)?.let { currentState ->
            val currentSelectedIds = currentState.selectedBookIds
            val newSelectedIds = if (currentSelectedIds.contains(bookId)) {
                currentSelectedIds - bookId
            } else {
                currentSelectedIds + bookId
            }
            _uiState.value = currentState.copy(selectedBookIds = newSelectedIds)

            if (newSelectedIds.isEmpty()) {
                _uiState.value = currentState.copy(isSelectionMode = false, selectedBookIds = emptySet())
            }
        }
    }

    fun startSelectionMode(bookId: String) {
        (_uiState.value as? ProfileUiState.Success)?.let { currentState ->
            _uiState.value = currentState.copy(
                isSelectionMode = true,
                selectedBookIds = setOf(bookId)
            )
        }
    }

    fun clearSelectionMode() {
        (_uiState.value as? ProfileUiState.Success)?.let { currentState ->
            _uiState.value = currentState.copy(
                isSelectionMode = false,
                selectedBookIds = emptySet()
            )
        }
    }

    fun deleteSelectedFavoriteBooks() {
        val currentState = (_uiState.value as? ProfileUiState.Success) ?: return
        val bookIdsToDelete = currentState.selectedBookIds
        val userId = currentUserId ?: return

        if (bookIdsToDelete.isEmpty()) return

        viewModelScope.launch {
            try {
                bookRepository.deleteFavoriteBooks(userId, bookIdsToDelete.toList())

                val updatedBooks = currentState.favoriteBooks.filterNot { it.isbn in bookIdsToDelete }
                _uiState.value = currentState.copy(
                    favoriteBooks = updatedBooks,
                    isSelectionMode = false,
                    selectedBookIds = emptySet()
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to delete favorite books", e)
            }
        }
    }
}
