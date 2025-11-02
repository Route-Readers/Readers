package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.UserPreferencesRepository // 이 Repository는 새로 만들어야 합니다.
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// UserPreferencesRepository를 생성자 주입으로 받습니다.
class AccountViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ProfileViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    // DataStore의 dark mode 설정을 읽어와 StateFlow로 변환합니다.
    val isDarkMode: StateFlow<Boolean> = userPreferencesRepository.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false // 초기값
        )

    init {
        if (currentUserId != null) {
            fetchUserProfile(currentUserId)
        }
    }

    fun updateUserPrivacySetting(isPrivate: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val userRef = db.collection("users").document(userId)
            userRef.update("isPrivate", isPrivate)
                .addOnSuccessListener {
                    fetchUserProfile(userId)
                }
                .addOnFailureListener {
                    // 실패 시에도 UI를 현재 서버 상태와 동기화하기 위해 호출
                    fetchUserProfile(userId)
                }
        }
    }

    // 다크 모드 설정을 업데이트하는 함수
    fun updateDarkModeSetting(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDarkMode(isDark)
        }
    }
}
