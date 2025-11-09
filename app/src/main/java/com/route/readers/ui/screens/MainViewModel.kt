package com.route.readers.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.User
import com.route.readers.data.remote.MyLibraryRepository
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
    private val myLibraryRepository = MyLibraryRepository()

    private val _consecutiveDays = MutableStateFlow(0)
    val consecutiveDays = _consecutiveDays.asStateFlow()

    private val _tokens = MutableStateFlow(0)
    val tokens = _tokens.asStateFlow()

    init {
        checkAndUpdateAttendance()
        loadUserTokens()
    }

    private fun checkAndUpdateAttendance() {
        if (currentUserId == null) {
            _consecutiveDays.value = 0
            return
        }

        viewModelScope.launch {
            try {
                val userRef = db.collection("users").document(currentUserId)
                val userDoc = userRef.get().await()
                val user = userDoc.toObject(User::class.java)

                if (user == null) {
                    _consecutiveDays.value = 0
                    return@launch
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdf.format(Date())

                if (user.lastLoginDate == todayStr) {
                    _consecutiveDays.value = user.consecutiveDays
                    return@launch
                }

                var newConsecutiveDays = 1
                val lastLoginDateStr = user.lastLoginDate

                if (lastLoginDateStr.isNotEmpty()) {
                    try {
                        val lastLoginCalendar = Calendar.getInstance().apply {
                            time = sdf.parse(lastLoginDateStr) ?: Date()
                        }
                        val yesterdayCalendar = Calendar.getInstance().apply {
                            add(Calendar.DATE, -1)
                        }

                        val isYesterday = lastLoginCalendar.get(Calendar.YEAR) == yesterdayCalendar.get(Calendar.YEAR) &&
                                lastLoginCalendar.get(Calendar.DAY_OF_YEAR) == yesterdayCalendar.get(Calendar.DAY_OF_YEAR)

                        if (isYesterday) {
                            newConsecutiveDays = user.consecutiveDays + 1
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error parsing lastLoginDate: $lastLoginDateStr", e)
                        newConsecutiveDays = 1
                    }
                }

                userRef.update(
                    mapOf(
                        "lastLoginDate" to todayStr,
                        "consecutiveDays" to newConsecutiveDays
                    )
                ).await()

                _consecutiveDays.value = newConsecutiveDays

            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to check or update attendance", e)
                _consecutiveDays.value = 0
            }
        }
    }

    private fun loadUserTokens() {
        if (currentUserId == null) {
            _tokens.value = 0
            return
        }

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val user = userDoc.toObject(User::class.java)
                _tokens.value = user?.tokens ?: 0
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load user tokens", e)
                _tokens.value = 0
            }
        }
    }

    fun addReadingTime(bookId: String, timeInSeconds: Int) {
        viewModelScope.launch {
            myLibraryRepository.addReadingTime(bookId, timeInSeconds)
        }
    }
}
