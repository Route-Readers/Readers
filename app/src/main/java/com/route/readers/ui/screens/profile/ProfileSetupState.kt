package com.route.readers.ui.screens.profile

sealed class ProfileSetupState {
    data object Idle : ProfileSetupState()
    data object Loading : ProfileSetupState()
    data object Success : ProfileSetupState()
    data class Error(val message: String) : ProfileSetupState()
}
