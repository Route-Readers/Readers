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
        val currentUserFollowingIds: Set<String>,
        val pendingFollowRequests: Set<String>
    ) : FollowListUiState()
    data class Error(val message: String) : FollowListUiState()
}

class FollowListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private var followStatusListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var outgoingFollowRequestListener: com.google.firebase.firestore.ListenerRegistration? = null

    // Separate StateFlows for real-time updates from listeners
    private val _currentUserFollowingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _pendingFollowRequests = MutableStateFlow<Set<String>>(emptySet())
    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    private val _blockedByUsers = MutableStateFlow<Set<String>>(emptySet())

    private val _usersFlow = MutableStateFlow<List<User>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState = combine(
        _usersFlow,
        _currentUserFollowingIds,
        _pendingFollowRequests,
        _loading,
        _error
    ) { users, followingIds, pendingRequests, loading, error ->
        if (loading) FollowListUiState.Loading
        else if (error != null) FollowListUiState.Error(error)
        else FollowListUiState.Success(users, followingIds, pendingRequests)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FollowListUiState.Loading
    )


    private var currentListOwnerId: String? = null
    private var currentListType: String? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchedUsers = combine(_searchQuery, uiState) { query, state -> // Use uiState here, not _uiState
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

    init {
        setupFollowStatusListener()
    }

    override fun onCleared() {
        super.onCleared()
        followStatusListener?.remove()
        outgoingFollowRequestListener?.remove()
    }

    private fun setupFollowStatusListener() {
        currentUserId?.let { userId ->
            val userRef = db.collection("users").document(userId)

            followStatusListener = userRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = "팔로우 상태를 가져오는 중 오류가 발생했습니다: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val newCurrentUserFollowingIds = (snapshot.get("following") as? List<String>)?.toSet() ?: emptySet()
                    _currentUserFollowingIds.value = newCurrentUserFollowingIds
                    _blockedUsers.value = (snapshot.get("blockedUsers") as? List<String>)?.toSet() ?: emptySet()
                    _blockedByUsers.value = (snapshot.get("blockedBy") as? List<String>)?.toSet() ?: emptySet()

                    // Check if any pending requests have been accepted
                    val acceptedRequests = _pendingFollowRequests.value.filter {
                        it in newCurrentUserFollowingIds
                    }.toSet()

                    if (acceptedRequests.isNotEmpty()) {
                        _pendingFollowRequests.value = _pendingFollowRequests.value - acceptedRequests
                    }
                }
                // No direct _uiState update here. uiState will react via combine.
            }

            val outgoingRequestsRef = userRef.collection("outgoingFollowRequests")
            outgoingFollowRequestListener = outgoingRequestsRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = "보낸 팔로우 요청을 가져오는 중 오류가 발생했습니다: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _pendingFollowRequests.value = snapshot.documents.map { it.id }.toSet()
                }
                // No direct _uiState update here. uiState will react via combine.
            }
        }
    }

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
            _loading.value = true
            _error.value = null
            if (currentUserId == null) {
                _error.value = "로그인이 필요합니다."
                _loading.value = false
                return@launch
            }

            try {
                val blockedUsers = _blockedUsers.value
                val blockedByUsers = _blockedByUsers.value
                val exclusionSet = blockedUsers + blockedByUsers

                val targetUserDoc = db.collection("users").document(userId).get().await()
                if (!targetUserDoc.exists()) {
                    _error.value = "사용자를 찾을 수 없습니다."
                    _loading.value = false
                    return@launch
                }

                val userIds = (targetUserDoc.get(listType) as? List<String>)?.filterNot { it in exclusionSet }
                if (userIds.isNullOrEmpty()) {
                    _usersFlow.value = emptyList()
                    _loading.value = false
                    return@launch
                }

                val fetchedUsers = mutableListOf<User>()
                userIds.chunked(30).forEach { chunk ->
                    val usersQuery = db.collection("users").whereIn("uid", chunk).get().await()
                    fetchedUsers.addAll(usersQuery.toObjects(User::class.java))
                }

                _usersFlow.value = fetchedUsers

            } catch (e: Exception) {
                _error.value = "목록을 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _loading.value = false
            }
        }

    }

    fun loadAllUsers() {
        this.currentListOwnerId = null
        this.currentListType = "all"
        _searchQuery.value = ""

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            if (currentUserId == null) {
                _error.value = "로그인이 필요합니다."
                _loading.value = false
                return@launch
            }

            try {
                val blockedUsers = _blockedUsers.value
                val blockedByUsers = _blockedByUsers.value
                val exclusionSet = blockedUsers + blockedByUsers

                val allUsersQuery = db.collection("users").limit(100).get().await()
                val allUsers = allUsersQuery.toObjects(User::class.java)
                    .filterNot { it.uid in exclusionSet } // 차단 관련 유저 필터링

                val sortedUsers = allUsers.sortedByDescending { it.uid == currentUserId }

                _usersFlow.value = sortedUsers
            } catch (e: Exception) {
                _error.value = "전체 사용자 목록을 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _loading.value = false
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
            val isRequestPending = currentState.pendingFollowRequests.contains(targetUserId) // Get isRequestPending here
            val currentUserRef = db.collection("users").document(currentUserId)
            val targetUserRef = db.collection("users").document(targetUserId)

            try {
                val targetUserDoc = targetUserRef.get().await()
                val targetUser = targetUserDoc.toObject(User::class.java)

                if (isRequestPending) {
                    cancelFollowRequest(targetUserId)
                    return@launch
                }
                
                if (targetUser?.isPrivate == true && !isCurrentlyFollowing) {
                    // Private account, send follow request
                    sendFollowRequest(targetUserId)
                    // Update UI to show "요청됨" (Requested) by adding to pendingFollowRequests
                    _pendingFollowRequests.value = currentState.pendingFollowRequests + targetUserId
                    return@launch
                }

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
                
                // If unfollowing a user who was previously a pending request (e.g., if the request was declined before being accepted)
                val updatedPendingRequests = if (isCurrentlyFollowing && currentState.pendingFollowRequests.contains(targetUserId)) {
                    currentState.pendingFollowRequests - targetUserId
                } else {
                    currentState.pendingFollowRequests
                }

                val updatedUsers = if (isCurrentlyFollowing && currentListType == "following") {
                    currentState.users.filter { it.uid != targetUserId }
                } else {
                    currentState.users.map { user ->
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
                }

            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    private suspend fun sendFollowRequest(targetUserId: String) {
        if (currentUserId == null) return
        try {
            val requestRef = db.collection("users").document(targetUserId)
                .collection("followRequests").document(currentUserId)

            val requestData = hashMapOf(
                "requesterId" to currentUserId,
                "timestamp" to FieldValue.serverTimestamp()
            )

            requestRef.set(requestData).await()

            // Update UI state to reflect that a request has been sent
            _pendingFollowRequests.value = _pendingFollowRequests.value + targetUserId
        } catch (e: Exception) {
            // Handle exception
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
                _usersFlow.value = updatedUsers
                _currentUserFollowingIds.value = currentState.currentUserFollowingIds - targetUserId
                _blockedUsers.value = _blockedUsers.value + targetUserId
                _blockedByUsers.value = _blockedByUsers.value + targetUserId

            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    private suspend fun cancelFollowRequest(targetUserId: String) {
        if (currentUserId == null) return
        try {
            // Remove from current user's outgoing requests
            db.collection("users").document(currentUserId)
                .collection("outgoingFollowRequests").document(targetUserId)
                .delete().await()

            // Remove from target user's incoming requests
            db.collection("users").document(targetUserId)
                .collection("followRequests").document(currentUserId)
                .delete().await()

            // Update UI state
            _pendingFollowRequests.value = _pendingFollowRequests.value - targetUserId
        } catch (e: Exception) {
            _error.value = "팔로우 요청 취소 중 오류가 발생했습니다: ${e.message}"
        }
    }
}
