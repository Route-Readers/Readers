package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _isPrivateAccount = MutableStateFlow(false)
    val isPrivateAccount: StateFlow<Boolean> = _isPrivateAccount

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchUserPrivacySetting()
    }

    // Firestore에서 사용자의 개인정보 설정을 가져옵니다.
    private fun fetchUserPrivacySetting() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // 'isPrivate' 필드가 있으면 그 값을, 없으면 false를 기본값으로 사용합니다.
                    _isPrivateAccount.value = document.getBoolean("isPrivate") ?: false
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
                // 오류 처리 (예: 로그 출력)
            }
    }

    // 사용자의 개인정보 설정을 Firestore에 업데이트합니다.
    fun updateUserPrivacySetting(isPrivate: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isPrivateAccount.value = isPrivate
            val userRef = db.collection("users").document(userId)
            userRef.update("isPrivate", isPrivate)
                .addOnSuccessListener {
                    // 성공 처리 (필요 시)
                }
                .addOnFailureListener {
                    // 실패 처리 (예: 이전 상태로 롤백)
                    fetchUserPrivacySetting() // 실패 시 다시 원래 값으로 돌림
                }
        }
    }
}
