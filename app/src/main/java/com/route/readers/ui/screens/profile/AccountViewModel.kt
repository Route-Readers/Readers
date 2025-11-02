package com.route.readers.ui.screens.profile

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AccountViewModel : ProfileViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

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
                    fetchUserProfile(userId)
                }
        }
    }
}
