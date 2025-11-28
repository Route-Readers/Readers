package com.route.readers.ui.screens.profile

sealed class PhoneVerificationState {
    data object Idle : PhoneVerificationState()
    data object Loading : PhoneVerificationState()
    data object CodeSent : PhoneVerificationState()
    data object Verified : PhoneVerificationState()
    data class Error(val message: String) : PhoneVerificationState()
}
