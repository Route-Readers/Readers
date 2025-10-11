package com.route.readers.data.manager // 패키지명은 프로젝트에 맞게 확인하세요.

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object BlockedUserManager {
    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds = _blockedUserIds.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    // 앱 시작 시 또는 로그인 시 호출하여 차단 목록을 미리 로드합니다.
    fun loadBlockedUsers() {
        val userId = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                val document = db.collection("users").document(userId).get().await()
                val ids = document.get("blockedUsers") as? List<String>
                _blockedUserIds.value = ids?.toSet() ?: emptySet()
            } catch (e: Exception) {
                // 에러 처리 (예: 로그 출력)
                _blockedUserIds.value = emptySet()
            }
        }
    }

    // 특정 유저가 차단되었는지 확인
    fun isBlocked(userId: String): Boolean {
        return _blockedUserIds.value.contains(userId)
    }

    // 사용자가 차단을 해제할 때 호출할 함수 (추가됨)
    fun unblockUser(userIdToUnblock: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                val userRef = db.collection("users").document(currentUserId)

                // Firestore에서 해당 유저 ID 제거
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    val blockedUsers = snapshot.get("blockedUsers") as? MutableList<String> ?: mutableListOf()
                    if (blockedUsers.contains(userIdToUnblock)) {
                        blockedUsers.remove(userIdToUnblock)
                        transaction.update(userRef, "blockedUsers", blockedUsers)
                    }
                    null
                }.await()

                // 로컬 StateFlow에서도 제거
                val updatedIds = _blockedUserIds.value.toMutableSet()
                updatedIds.remove(userIdToUnblock)
                _blockedUserIds.value = updatedIds

            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }


    // 로그아웃 시 호출하여 메모리 정리
    fun clear() {
        _blockedUserIds.value = emptySet()
    }
}
