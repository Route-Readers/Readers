package com.route.readers.ui.screens.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.UserPreferencesRepository
import com.route.readers.data.model.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential

class AccountViewModel(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository
) : ProfileViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    val isDarkMode: StateFlow<Boolean?> = userPreferencesRepository.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        if (currentUserId != null) {
            fetchUserProfile(currentUserId)
        }
    }

    fun syncWithSystemTheme(isSystemInDark: Boolean) {
        viewModelScope.launch {
            if (userPreferencesRepository.isDarkMode.first() == null) {
                userPreferencesRepository.updateDarkMode(isSystemInDark)
            }
        }
    }

    fun updateUserPrivacySetting(isPrivate: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("private", isPrivate)
                    .await()
                
                fetchUserProfile(userId)
                Log.d("AccountViewModel", "User privacy setting updated successfully.")
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Failed to update user privacy setting.", e)
            }
        }
    }

    fun updateDarkModeSetting(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDarkMode(isDark)
        }
    }

    sealed class DeleteAccountResult {
        data object Success : DeleteAccountResult()
        data class Failure(val message: String) : DeleteAccountResult()
        data object ReauthenticationRequired : DeleteAccountResult()
        data object GoogleReauthenticationRequired : DeleteAccountResult()
    }

    // Function to delete user account
    fun deleteAccount() {
        val user = auth.currentUser
        if (user == null) {
            _deleteAccountResult.value = DeleteAccountResult.Failure("인증된 사용자가 없습니다.")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Delete user data from Firestore (simplified)
                db.collection("users").document(user.uid).delete().await()
                Log.d("AccountViewModel", "User document deleted from Firestore.")

                // 2. Attempt to delete user from Firebase Authentication
                user.delete().await()
                Log.d("AccountViewModel", "Firebase Auth user deleted successfully.")

                // 3. Sign out the user
                auth.signOut()

                // Final check to ensure the user is indeed null after deletion and sign-out
                if (auth.currentUser == null) {
                    _deleteAccountResult.value = DeleteAccountResult.Success
                } else {
                    Log.e("AccountViewModel", "Firebase Auth user deletion reported success, but current user is not null.")
                    _deleteAccountResult.value = DeleteAccountResult.Failure("계정 삭제 후 사용자 정보가 남아있습니다. 다시 시도해주세요.")
                }

            } catch (e: Exception) {
                Log.e("AccountViewModel", "Error deleting account", e)
                val isGoogleUser = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

                if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    if (isGoogleUser) {
                        // For Google users, reauthentication with password is not applicable.
                        // We need to trigger a Google reauthentication flow in the UI.
                        _deleteAccountResult.value = DeleteAccountResult.GoogleReauthenticationRequired
                    } else {
                        // For non-Google users (e.g., email/password), reauthentication with password is required.
                        _deleteAccountResult.value = DeleteAccountResult.ReauthenticationRequired
                    }
                } else {
                    _deleteAccountResult.value = DeleteAccountResult.Failure("계정 삭제에 실패했습니다: ${e.message}")
                }
            }
        }
    }

    private val _deleteAccountResult = MutableStateFlow<DeleteAccountResult?>(null)
    val deleteAccountResult: StateFlow<DeleteAccountResult?> = _deleteAccountResult.asStateFlow()

    fun reauthenticateAndThenDelete(credential: com.google.firebase.auth.AuthCredential) {
        val user = auth.currentUser
        if (user == null) {
            _deleteAccountResult.value = DeleteAccountResult.Failure("인증된 사용자가 없습니다.")
            return
        }

        viewModelScope.launch {
            try {
                user.reauthenticate(credential).await()
                Log.d("AccountViewModel", "User reauthenticated successfully.")
                // If reauthentication is successful, retry deletion
                deleteAccount()
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Reauthentication failed", e)
                _deleteAccountResult.value = DeleteAccountResult.Failure("재인증 실패: ${e.message}")
            }
        }
    }
    
    // New function to handle Google reauthentication
    fun reauthenticateWithGoogleAndThenDelete(credential: AuthCredential) {
        val user = auth.currentUser
        if (user == null) {
            _deleteAccountResult.value = DeleteAccountResult.Failure("인증된 사용자가 없습니다.")
            return
        }

        viewModelScope.launch {
            try {
                user.reauthenticate(credential).await()
                Log.d("AccountViewModel", "User reauthenticated with Google successfully.")
                // If reauthentication is successful, retry deletion
                deleteAccount()
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Google reauthentication failed", e)
                _deleteAccountResult.value = DeleteAccountResult.Failure("Google 재인증 실패: ${e.message}")
            }
        }
    }

    fun setDeleteAccountResult(result: DeleteAccountResult?) {
        _deleteAccountResult.value = result
    }
}
