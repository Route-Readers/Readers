package com.route.readers.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object ExistingUser : LoginUiState()
    object NewUser : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("이메일과 비밀번호를 모두 입력해주세요.")
            return
        }
        _uiState.value = LoginUiState.Loading

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        checkIfUserProfileExists(user.uid)
                    } else {
                        _uiState.value = LoginUiState.Error("이메일 인증을 먼저 완료해주세요.")
                        auth.signOut()
                    }
                } else {
                    val exceptionMessage = task.exception?.message
                    val errorMessage = when {
                        exceptionMessage?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "이메일 또는 비밀번호가 잘못되었습니다."
                        else -> "로그인에 실패했습니다."
                    }
                    _uiState.value = LoginUiState.Error(errorMessage)
                }
            }
    }

    fun handleSuccessfulAuth(authResult: AuthResult) {
        val firebaseUser = authResult.user
        if (firebaseUser == null) {
            _uiState.value = LoginUiState.Error("사용자 정보를 가져오는데 실패했습니다.")
            return
        }
        checkIfUserProfileExists(firebaseUser.uid)
    }

    private fun checkIfUserProfileExists(uid: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val document = db.collection("users").document(uid).get().await()
                if (document.exists()) {
                    _uiState.value = LoginUiState.ExistingUser
                } else {
                    _uiState.value = LoginUiState.NewUser
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("사용자 정보 확인 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
