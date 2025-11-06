package com.route.readers.ui.screens.attendance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AttendanceData(
    val date: LocalDate,
    val points: Int = 10,
    val event: String? = null,
    val consecutiveAttendanceDays: Int = 1
)

class AttendanceViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _attendanceData = MutableStateFlow<Map<LocalDate, AttendanceData>>(emptyMap())
    val attendanceData = _attendanceData.asStateFlow()

    private val _consecutiveReadingDays = MutableStateFlow(0)
    val consecutiveReadingDays: StateFlow<Int> = _consecutiveReadingDays.asStateFlow()

    val totalAttendanceDays: StateFlow<Int> = attendanceData.map { dataMap ->
        dataMap.values.count { it.points > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalReadingDays: StateFlow<Int> = attendanceData.map { dataMap ->
        dataMap.values.count { it.event != null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var isCheckingAttendance = false

    init {
        refreshAttendanceData()
    }

    fun markReadingActivity() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            try {
                val userDocRef = db.collection("users").document(userId)
                val document = userDocRef.get().await()

                val attendanceList = document["attendance"] as? List<HashMap<String, Any>> ?: emptyList()
                val todayRecordIndex = attendanceList.indexOfFirst { it["date"] == todayStr }

                if (todayRecordIndex != -1) {
                    val updatedList = attendanceList.toMutableList()
                    val todayData = updatedList[todayRecordIndex].toMutableMap()
                    todayData["event"] = "leaf_1"
                    updatedList[todayRecordIndex] = todayData as HashMap<String, Any>

                    userDocRef.update("attendance", updatedList).await()
                } else {
                    val yesterday = today.minusDays(1)
                    val lastConsecutiveAttendance = _attendanceData.value[yesterday]?.consecutiveAttendanceDays ?: 0
                    val newReadingRecord = hashMapOf(
                        "date" to todayStr,
                        "points" to 0,
                        "event" to "leaf_1",
                        "consecutiveAttendanceDays" to lastConsecutiveAttendance
                    )
                    userDocRef.update("attendance", FieldValue.arrayUnion(newReadingRecord)).await()
                }

                refreshAttendanceData()

            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error marking reading activity", e)
            }
        }
    }

    fun checkAttendance() {
        if (isCheckingAttendance) return
        viewModelScope.launch {
            isCheckingAttendance = true
            val userId = auth.currentUser?.uid ?: run {
                isCheckingAttendance = false
                return@launch
            }
            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val todayData = _attendanceData.value[today]
            if (todayData != null && todayData.points > 0) {
                isCheckingAttendance = false
                return@launch
            }

            try {
                val userDocRef = db.collection("users").document(userId)

                val yesterday = today.minusDays(1)
                val lastConsecutiveAttendance = _attendanceData.value[yesterday]?.consecutiveAttendanceDays ?: 0
                val newConsecutiveAttendance = lastConsecutiveAttendance + 1

                val document = userDocRef.get().await()
                val attendanceList = (document["attendance"] as? List<HashMap<String, Any>>)?.toMutableList() ?: mutableListOf()

                val todayRecordIndex = attendanceList.indexOfFirst { it["date"] == todayStr }

                if (todayRecordIndex != -1) {
                    val recordToUpdate = attendanceList[todayRecordIndex]
                    recordToUpdate["points"] = 10
                    recordToUpdate["consecutiveAttendanceDays"] = newConsecutiveAttendance
                    attendanceList[todayRecordIndex] = recordToUpdate
                    userDocRef.update("attendance", attendanceList).await()
                } else {
                    val newAttendanceRecord = hashMapOf(
                        "date" to todayStr,
                        "points" to 10,
                        "event" to null,
                        "consecutiveAttendanceDays" to newConsecutiveAttendance
                    )
                    userDocRef.update("attendance", FieldValue.arrayUnion(newAttendanceRecord)).await()
                }

                refreshAttendanceData()

            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error checking attendance", e)
            } finally {
                isCheckingAttendance = false
            }
        }
    }

    fun refreshAttendanceData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val document = db.collection("users").document(userId).get().await()
                val dataFromFirestore = document["attendance"] as? List<Map<String, Any>> ?: emptyList()

                val parsedData = dataFromFirestore.mapNotNull { data ->
                    val dateStr = data["date"] as? String
                    val points = (data["points"] as? Long)?.toInt() ?: 0
                    val event = data["event"] as? String
                    val consecutiveAttendanceDays = (data["consecutiveAttendanceDays"] as? Long)?.toInt() ?: 0

                    if (dateStr != null) {
                        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                        date to AttendanceData(date, points, event, consecutiveAttendanceDays)
                    } else {
                        null
                    }
                }.toMap()

                _attendanceData.value = parsedData
                calculateConsecutiveReadingDays(parsedData)

            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error refreshing attendance data", e)
            }
        }
    }

    private fun calculateConsecutiveReadingDays(data: Map<LocalDate, AttendanceData>) {
        if (data.isEmpty()) {
            _consecutiveReadingDays.value = 0
            return
        }

        val sortedDates = data.keys.sortedDescending()
        var currentDate = LocalDate.now()
        var consecutiveCount = 0

        val todayRecord = data[currentDate]
        val isTodayRead = todayRecord != null && todayRecord.event != null

        if (!isTodayRead) {
            currentDate = currentDate.minusDays(1)
        }

        for (date in sortedDates) {
            if (date.isAfter(currentDate)) continue

            if (date == currentDate) {
                val record = data[date]
                if (record != null && record.event != null) {
                    consecutiveCount++
                    currentDate = currentDate.minusDays(1)
                } else {
                    break
                }
            } else {
                break
            }
        }
        _consecutiveReadingDays.value = consecutiveCount
    }
}