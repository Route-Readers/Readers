package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.UserPreferencesRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
            val userRef = db.collection("users").document(userId)
            userRef.update("isPrivate", isPrivate)
                .addOnSuccessListener {
                    fetchUserProfile(userId)
                }
                .addOnFailureListener {
                    fetchUserProfile(userId)
                }
        }
    }

    fun updateDarkModeSetting(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDarkMode(isDark)
        }
    }
}
