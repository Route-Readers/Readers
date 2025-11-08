package com.route.readers.ui.screens.profile

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

class AccountViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ProfileViewModel() {

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
                val userRef = db.collection("users").document(userId)
                val user = userRef.get().await().toObject(User::class.java)
                if (user != null) {
                    val updatedUser = user.copy(isPrivate = isPrivate)
                    userRef.set(updatedUser).await()
                    val currentState = _uiState.value
                    if (currentState is ProfileUiState.Success) {
                        _uiState.value = currentState.copy(user = updatedUser)
                    }
                    Log.d("AccountViewModel", "User privacy setting updated successfully.")
                }
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
}