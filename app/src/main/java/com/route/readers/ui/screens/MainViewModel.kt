package com.route.readers.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val _consecutiveDays = MutableStateFlow(0)
    val consecutiveDays = _consecutiveDays.asStateFlow()

    init {
        checkAndUpdateAttendance()
    }

    private fun checkAndUpdateAttendance() {
        if (currentUserId == null) return

        viewModelScope.launch {
            try {
                val userRef = db.collection("users").document(currentUserId)
                val userDoc = userRef.get().await()
                val user = userDoc.toObject(User::class.java) ?: return@launch

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                if (user.lastLoginDate == today) {
                    _consecutiveDays.value = user.consecutiveDays
                    return@launch
                }

                val lastLoginDate = user.lastLoginDate
                var newConsecutiveDays = 1

                if (lastLoginDate.isNotEmpty()) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val lastDate = sdf.parse(lastLoginDate)
                    val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time
                    val yesterdayStr = sdf.format(yesterday)

                    if (lastLoginDate == yesterdayStr) {
                        newConsecutiveDays = user.consecutiveDays + 1
                    }
                }

                userRef.update(
                    mapOf(
                        "lastLoginDate" to today,
                        "consecutiveDays" to newConsecutiveDays
                    )
                ).await()

                _consecutiveDays.value = newConsecutiveDays

            } catch (e: Exception) {
                _consecutiveDays.value = 0
            }
        }
    }
}
