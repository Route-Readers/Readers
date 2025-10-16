package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

    private fun fetchBlockedUsers() {
        if (currentUserId == null) {
            _uiState.value = BlockedUserUiState.Error("로그인이 필요합니다.")
            return
        }

        viewModelScope.launch {
            _uiState.value = BlockedUserUiState.Loading
            try {
                // 1. 현재 사용자의 문서에서 'blockedUsers' 필드(ID 목록)를 가져옵니다.
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val blockedUserIds = userDoc.get("blockedUsers") as? List<String> ?: emptyList()

                if (blockedUserIds.isEmpty()) {
                    // 2. 차단한 사용자가 없으면 빈 목록으로 Success 상태를 설정합니다.
                    _uiState.value = BlockedUserUiState.Success(emptyList())
                } else {
                    // 3. 차단된 사용자 ID 목록을 사용하여 사용자 정보(User 객체)를 가져옵니다.
                    // Firestore 'whereIn' 쿼리는 한 번에 30개까지의 ID만 처리할 수 있으므로, 10개 또는 30개 단위로 나누어 요청하는 것이 안전합니다.
                    val users = blockedUserIds.chunked(30).flatMap { chunk ->
                        db.collection("users").whereIn("uid", chunk).get().await().toObjects(User::class.java)
                    }
                    _uiState.value = BlockedUserUiState.Success(users)
                }
            } catch (e: Exception) {
                _uiState.value = BlockedUserUiState.Error("차단 목록을 불러오는 데 실패했습니다: ${e.message}")
            }
        }
    }

    fun unblockUser(targetUserId: String) {
        if (currentUserId == null) {
            _uiState.value = BlockedUserUiState.Error("로그인이 필요합니다.")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Firestore에서 해당 사용자를 'blockedUsers' 배열에서 제거합니다.
                db.collection("users").document(currentUserId)
                    .update("blockedUsers", FieldValue.arrayRemove(targetUserId))
                    .await()

                // 2. UI 상태를 즉시 갱신하여 차단 해제된 사용자를 목록에서 제거합니다.
                if (_uiState.value is BlockedUserUiState.Success) {
                    val currentUsers = (_uiState.value as BlockedUserUiState.Success).users
                    val updatedUsers = currentUsers.filterNot { it.uid == targetUserId }
                    _uiState.value = BlockedUserUiState.Success(updatedUsers)
                }

            } catch (e: Exception) {
                // 필요 시 에러 상태를 UI에 표시할 수 있습니다.
                // _uiState.value = BlockedUserUiState.Error("차단 해제에 실패했습니다: ${e.message}")
            }
        }
    }
}
