package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.manager.BlockedUserManager
import com.route.readers.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class BlockedUserUiState {
    data object Loading : BlockedUserUiState()
    data class Success(val users: List<User>) : BlockedUserUiState()
    data class Error(val message: String) : BlockedUserUiState()
}

class BlockedUserViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val _uiState = MutableStateFlow<BlockedUserUiState>(BlockedUserUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchBlockedUsers()
    }

    fun fetchBlockedUsers() {
        if (currentUserId == null) {
            _uiState.value = BlockedUserUiState.Error("로그인이 필요합니다.")
            return
        }
        viewModelScope.launch {
            _uiState.value = BlockedUserUiState.Loading
            try {
                val blockedIds = BlockedUserManager.blockedUserIds.value
                if (blockedIds.isEmpty()) {
                    _uiState.value = BlockedUserUiState.Success(emptyList())
                    return@launch
                }

                val chunkedBlockedIds = blockedIds.toList().chunked(10)
                val users = mutableListOf<User>()

                for (chunk in chunkedBlockedIds) {
                    val usersSnapshot = db.collection("users").whereIn("uid", chunk).get().await()
                    users.addAll(usersSnapshot.toObjects(User::class.java))
                }

                _uiState.value = BlockedUserUiState.Success(users)

            } catch (e: Exception) {
                _uiState.value = BlockedUserUiState.Error("차단 목록을 불러오는데 실패했습니다.")
            }
        }
    }

    fun unblockUser(targetUserId: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            try {
                db.collection("users").document(currentUserId)
                    .update("blockedUsers", FieldValue.arrayRemove(targetUserId))
                    .await()

                // 전역 차단 목록에서 제거하고 현재 화면 갱신
                // BlockedUserManager.unblockUser(targetUserId) // 이 함수가 없으므로 주석 처리
                fetchBlockedUsers()

            } catch (e: Exception) {
                // 오류 처리 (예: 스낵바 메시지 표시)
            }
        }
    }
}
