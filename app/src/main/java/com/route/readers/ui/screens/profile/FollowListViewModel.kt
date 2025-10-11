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
        val allUsers: List<User>,
        val currentUserFollowingIds: Set<String>,
        val currentUserFollowerIds: Set<String>
    ) : FollowListUiState()

    data class Error(val message: String) : FollowListUiState()
}

class FollowListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<FollowListUiState>(FollowListUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentListOwnerId: String? = null
    private var currentListType: String? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchedUsers = _searchQuery
        .combine(_uiState) { query, state ->
            if (state is FollowListUiState.Success) {
                if (query.isBlank()) {
                    state.allUsers
                } else {
                    state.allUsers.filter {
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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun loadListForTab(userId: String, tabIndex: Int) {
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
            val loggedInUserId = auth.currentUser?.uid
            if (loggedInUserId == null) {
                _uiState.value = FollowListUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            try {
                val currentUserDoc = db.collection("users").document(loggedInUserId).get().await()
                val currentUserFollowingIds = (currentUserDoc.get("following") as? List<String>)?.toSet() ?: emptySet()
                val currentUserFollowerIds = (currentUserDoc.get("followers") as? List<String>)?.toSet() ?: emptySet()

                val targetUserDoc = db.collection("users").document(userId).get().await()
                if (!targetUserDoc.exists()) {
                    _uiState.value = FollowListUiState.Error("사용자를 찾을 수 없습니다.")
                    return@launch
                }

                val userIds = targetUserDoc.get(listType) as? List<String>

                if (userIds.isNullOrEmpty()) {
                    _uiState.value = FollowListUiState.Success(emptyList(), currentUserFollowingIds, currentUserFollowerIds)
                    return@launch
                }

                val fetchedUsers = mutableListOf<User>()
                userIds.chunked(30).forEach { chunk ->
                    val usersQuery = db.collection("users").whereIn("uid", chunk).get().await()
                    val users = usersQuery.toObjects(User::class.java)
                    fetchedUsers.addAll(users.filter { it.nickname.isNotBlank() })
                }

                _uiState.value = FollowListUiState.Success(fetchedUsers, currentUserFollowingIds, currentUserFollowerIds)

            } catch (e: Exception) {
                _uiState.value = FollowListUiState.Error("목록을 불러오는 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    private fun loadAllUsers() {
        this.currentListOwnerId = null
        this.currentListType = "all"
        _searchQuery.value = ""

        viewModelScope.launch {
            _uiState.value = FollowListUiState.Loading
            val loggedInUserId = auth.currentUser?.uid
            if (loggedInUserId == null) {
                _uiState.value = FollowListUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            try {
                val currentUserDoc = db.collection("users").document(loggedInUserId).get().await()
                val currentUserFollowingIds = (currentUserDoc.get("following") as? List<String>)?.toSet() ?: emptySet()
                val currentUserFollowerIds = (currentUserDoc.get("followers") as? List<String>)?.toSet() ?: emptySet()

                val allUsersQuery = db.collection("users").limit(100).get().await()
                val allUsers = allUsersQuery.toObjects(User::class.java)

                val filteredUsers = allUsers.filter { it.nickname.isNotBlank() }

                _uiState.value = FollowListUiState.Success(filteredUsers, currentUserFollowingIds, currentUserFollowerIds)
            } catch (e: Exception) {
                _uiState.value = FollowListUiState.Error("전체 사용자 목록을 불러오는 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    fun refresh() {
        val uid = currentListOwnerId
        val type = currentListType
        if (type == "all") {
            loadAllUsers()
        } else if (uid != null && type != null) {
            loadFollowList(uid, type)
        }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            val loggedInUserId = auth.currentUser?.uid ?: return@launch
            if (loggedInUserId == targetUserId) return@launch

            val currentState = uiState.value as? FollowListUiState.Success ?: return@launch

            val isCurrentlyFollowing = currentState.currentUserFollowingIds.contains(targetUserId)

            // --- UI 즉시 업데이트 로직 ---
            val newFollowingIds = if (isCurrentlyFollowing) {
                currentState.currentUserFollowingIds - targetUserId
            } else {
                currentState.currentUserFollowingIds + targetUserId
            }

            // '팔로잉' 탭에서 언팔로우 시 목록에서 즉시 제거
            val newUsersList = if (isCurrentlyFollowing && currentListType == "following" && currentListOwnerId == loggedInUserId) {
                currentState.allUsers.filterNot { it.uid == targetUserId }
            } else {
                currentState.allUsers
            }

            _uiState.value = currentState.copy(
                allUsers = newUsersList,
                currentUserFollowingIds = newFollowingIds
            )
            // --- UI 즉시 업데이트 로직 끝 ---

            // --- 서버 데이터 업데이트 로직 (백그라운드) ---
            val currentUserRef = db.collection("users").document(loggedInUserId)
            val targetUserRef = db.collection("users").document(targetUserId)

            try {
                db.runTransaction { transaction ->
                    if (isCurrentlyFollowing) { // 언팔로우
                        transaction.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
                        transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                        transaction.update(targetUserRef, "followers", FieldValue.arrayRemove(loggedInUserId))
                        transaction.update(targetUserRef, "followerCount", FieldValue.increment(-1))
                    } else { // 팔로우
                        transaction.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))
                        transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                        transaction.update(targetUserRef, "followers", FieldValue.arrayUnion(loggedInUserId))
                        transaction.update(targetUserRef, "followerCount", FieldValue.increment(1))
                    }
                    null
                }.await()
            } catch (e: Exception) {
                // 서버 업데이트 실패 시 UI 롤백
                _uiState.value = currentState
            }
            // --- 서버 데이터 업데이트 로직 끝 ---
        }
    }
}
