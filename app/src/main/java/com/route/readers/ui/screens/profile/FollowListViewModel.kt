package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class FollowListUiState {
    data object Loading : FollowListUiState()
    data class Success(
        val users: List<User>,
        val currentUserFollowingIds: Set<String>
    ) : FollowListUiState()
    data class Error(val message: String) : FollowListUiState()
}

class FollowListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val _uiState = MutableStateFlow<FollowListUiState>(FollowListUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentListOwnerId: String? = null
    private var currentListType: String? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchedUsers = combine(_searchQuery, _uiState) { query, state ->
        if (state is FollowListUiState.Success) {
            if (query.isBlank()) {
                state.users
            } else {
                state.users.filter {
                    it.nickname.contains(query, ignoreCase = true)
                }
            }
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isMyProfile = MutableStateFlow(false)
    val isMyProfile = _isMyProfile.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun loadListForTab(userId: String, tabIndex: Int) {
        _isMyProfile.value = userId == currentUserId
        when (tabIndex) {
            0 -> loadFollowList(userId, "followers")
            1 -> loadFollowList(userId, "following")
            2 -> loadAllUsers()
        }
    }

    private fun loadFollowList(userId: String, listType: String) {
        this.currentListOwnerId = userId
        this.currentListType = listType
        _searchQuery.value = ""

        viewModelScope.launch {
            _uiState.value = FollowListUiState.Loading
            if (currentUserId == null) {
                _uiState.value = FollowListUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            try {

                val currentUserDoc = db.collection("users").document(currentUserId).get().await()
                val currentUserFollowingIds = (currentUserDoc.get("following") as? List<String>)?.toSet() ?: emptySet()
                val blockedUsers = (currentUserDoc.get("blockedUsers") as? List<String>)?.toSet() ?: emptySet()
                val blockedByUsers = (currentUserDoc.get("blockedBy") as? List<String>)?.toSet() ?: emptySet()
                val exclusionSet = blockedUsers + blockedByUsers

                val targetUserDoc = db.collection("users").document(userId).get().await()
                if (!targetUserDoc.exists()) {
                    _uiState.value = FollowListUiState.Error("사용자를 찾을 수 없습니다.")
                    return@launch
                }

                val userIds = (targetUserDoc.get(listType) as? List<String>)?.filterNot { it in exclusionSet }
                if (userIds.isNullOrEmpty()) {
                    _uiState.value = FollowListUiState.Success(emptyList(), currentUserFollowingIds)
                    return@launch
                }

                val fetchedUsers = mutableListOf<User>()
                userIds.chunked(30).forEach { chunk ->
                    val usersQuery = db.collection("users").whereIn("uid", chunk).get().await()
                    fetchedUsers.addAll(usersQuery.toObjects(User::class.java))
                }

                _uiState.value = FollowListUiState.Success(fetchedUsers, currentUserFollowingIds)

            } catch (e: Exception) {
                _uiState.value = FollowListUiState.Error("목록을 불러오는 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    fun loadAllUsers() {
        this.currentListOwnerId = null
        this.currentListType = "all"
        _searchQuery.value = ""

        viewModelScope.launch {
            _uiState.value = FollowListUiState.Loading
            if (currentUserId == null) {
                _uiState.value = FollowListUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            try {

                val currentUserDoc = db.collection("users").document(currentUserId).get().await()
                val currentUserFollowingIds = (currentUserDoc.get("following") as? List<String>)?.toSet() ?: emptySet()
                val blockedUsers = (currentUserDoc.get("blockedUsers") as? List<String>)?.toSet() ?: emptySet()
                val blockedByUsers = (currentUserDoc.get("blockedBy") as? List<String>)?.toSet() ?: emptySet()
                val exclusionSet = blockedUsers + blockedByUsers

                val allUsersQuery = db.collection("users").limit(100).get().await()
                val allUsers = allUsersQuery.toObjects(User::class.java)
                    .filterNot { it.uid in exclusionSet } // 차단 관련 유저 필터링

                val sortedUsers = allUsers.sortedByDescending { it.uid == currentUserId }

                _uiState.value = FollowListUiState.Success(sortedUsers, currentUserFollowingIds)
            } catch (e: Exception) {
                _uiState.value = FollowListUiState.Error("전체 사용자 목록을 불러오는 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    fun refresh() {
        val uid = currentListOwnerId
        val type = currentListType
        if (uid != null && type != null && type != "all") {
            loadFollowList(uid, type)
        } else if (type == "all") {
            loadAllUsers()
        }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            if (currentUserId == null || currentUserId == targetUserId) {
                return@launch
            }

            val currentState = uiState.value
            if (currentState !is FollowListUiState.Success) {
                return@launch
            }

            val isCurrentlyFollowing = currentState.currentUserFollowingIds.contains(targetUserId)
            val currentUserRef = db.collection("users").document(currentUserId)
            val targetUserRef = db.collection("users").document(targetUserId)

            try {
                db.runTransaction { transaction ->
                    if (isCurrentlyFollowing) {
                        transaction.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
                        transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                        transaction.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))
                        transaction.update(targetUserRef, "followerCount", FieldValue.increment(-1))
                    } else {
                        transaction.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))
                        transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                        transaction.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))
                        transaction.update(targetUserRef, "followerCount", FieldValue.increment(1))
                        // 팔로우 알림 생성
                        val followNotificationRef = db.collection("feeds").document()
                        transaction.set(followNotificationRef, mapOf (
                            "type" to "FOLLOW_NOTIFICATION",
                            "followerId" to currentUserId,
                            "receiverId" to targetUserId,
                            "isFollowedBack" to false,
                            "timestamp" to FieldValue.serverTimestamp()
                        ))
                    }
                    null
                }.await()

                val updatedFollowingIds = if (isCurrentlyFollowing) {
                    currentState.currentUserFollowingIds - targetUserId
                } else {
                    currentState.currentUserFollowingIds + targetUserId
                }

                val updatedUsers = currentState.users.map { user ->
                    if (user.uid == currentUserId) {
                        val newFollowingCount = user.followingCount + if (isCurrentlyFollowing) -1 else 1
                        user.copy(followingCount = newFollowingCount)
                    }
                    else if (user.uid == targetUserId) {
                        val newFollowerCount = user.followerCount + if (isCurrentlyFollowing) -1 else 1
                        user.copy(followerCount = newFollowerCount)
                    }
                    else {
                        user
                    }
                }

                _uiState.value = currentState.copy(
                    users = updatedUsers,
                    currentUserFollowingIds = updatedFollowingIds
                )

            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            if (currentUserId == null || currentUserId == targetUserId) {
                return@launch
            }

            val currentState = uiState.value
            if (currentState !is FollowListUiState.Success) {
                return@launch
            }

            val currentUserRef = db.collection("users").document(currentUserId)
            val targetUserRef = db.collection("users").document(targetUserId)

            try {
                db.runTransaction { transaction ->
                    transaction.update(currentUserRef, "blockedUsers", FieldValue.arrayUnion(targetUserId))
                    transaction.update(targetUserRef, "blockedBy", FieldValue.arrayUnion(currentUserId))

                    if (currentState.currentUserFollowingIds.contains(targetUserId)) {
                        transaction.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
                        transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                        transaction.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))
                        transaction.update(targetUserRef, "followerCount", FieldValue.increment(-1))
                    }

                    val targetUserDoc = transaction.get(targetUserRef)
                    val targetUserFollowing = targetUserDoc.get("following") as? List<String> ?: emptyList()
                    if (targetUserFollowing.contains(currentUserId)) {
                        transaction.update(targetUserRef, "following", FieldValue.arrayRemove(currentUserId))
                        transaction.update(targetUserRef, "followingCount", FieldValue.increment(-1))
                        transaction.update(currentUserRef, "followers", FieldValue.arrayRemove(targetUserId))
                        transaction.update(currentUserRef, "followerCount", FieldValue.increment(-1))
                    }

                    null
                }.await()

                val updatedUsers = currentState.users.filterNot { it.uid == targetUserId }
                val updatedFollowingIds = currentState.currentUserFollowingIds - targetUserId

                _uiState.value = currentState.copy(
                    users = updatedUsers,
                    currentUserFollowingIds = updatedFollowingIds
                )

            } catch (e: Exception) {
                // Handle exception
            }
        }
    }
}
