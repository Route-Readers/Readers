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
    val points: Int = 0,
    val event: String? = null
)

class AttendanceViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid

    private val _attendanceData = MutableStateFlow<Map<LocalDate, AttendanceData>>(emptyMap())
    val attendanceData: StateFlow<Map<LocalDate, AttendanceData>> = _attendanceData.asStateFlow()

    private val _consecutiveReadingDays = MutableStateFlow(0)
    val consecutiveReadingDays: StateFlow<Int> = _consecutiveReadingDays.asStateFlow()

    private val _consecutiveAttendanceDays = MutableStateFlow(0)
    val consecutiveAttendanceDays: StateFlow<Int> = _consecutiveAttendanceDays.asStateFlow()

    val totalAttendanceDays: StateFlow<Int> = attendanceData.map { dataMap ->
        dataMap.values.count { it.points > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalReadingDays: StateFlow<Int> = attendanceData.map { dataMap ->
        dataMap.values.count { it.event != null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var isCheckingAttendance = false

    init {
        viewModelScope.launch {
            refreshAttendanceData()
        }
    }

    fun markReadingActivity() {
        viewModelScope.launch {
            userId ?: return@launch
            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            try {
                val userDocRef = db.collection("users").document(userId)
                val document = userDocRef.get().await()

                val attendanceList =
                    (document["attendance"] as? List<HashMap<String, Any>>)?.toMutableList()
                        ?: mutableListOf()
                val todayRecordIndex = attendanceList.indexOfFirst { it["date"] == todayStr }

                if (todayRecordIndex != -1) {
                    val todayData = attendanceList[todayRecordIndex]
                    if (todayData["event"] == null) {
                        todayData["event"] = "leaf_1"
                        userDocRef.update("attendance", attendanceList).await()
                    }
                } else {
                    val newReadingRecord = hashMapOf(
                        "date" to todayStr,
                        "points" to 0,
                        "event" to "leaf_1"
                    )
                    userDocRef.update("attendance", FieldValue.arrayUnion(newReadingRecord))
                        .await()
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
            userId ?: run {
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
                val document = userDocRef.get().await()
                val attendanceList =
                    (document["attendance"] as? List<HashMap<String, Any>>)?.toMutableList()
                        ?: mutableListOf()
                val todayRecordIndex = attendanceList.indexOfFirst { it["date"] == todayStr }

                if (todayRecordIndex != -1) {
                    val recordToUpdate = attendanceList[todayRecordIndex]
                    recordToUpdate["points"] = 10
                    attendanceList[todayRecordIndex] = recordToUpdate
                    userDocRef.update("attendance", attendanceList).await()
                } else {
                    val newAttendanceRecord = hashMapOf(
                        "date" to todayStr,
                        "points" to 10,
                        "event" to null,
                    )
                    userDocRef.update("attendance", FieldValue.arrayUnion(newAttendanceRecord))
                        .await()
                }
                refreshAttendanceData()
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error checking attendance", e)
            } finally {
                isCheckingAttendance = false
            }
        }
    }

    suspend fun refreshAttendanceData(): Pair<Int, Int> {
        userId ?: return 0 to 0
        return try {
            val document = db.collection("users").document(userId).get().await()
            val dataFromFirestore =
                document["attendance"] as? List<Map<String, Any>> ?: emptyList()

            val parsedData = dataFromFirestore.mapNotNull { data ->
                val dateStr = data["date"] as? String
                val points = (data["points"] as? Long)?.toInt() ?: 0
                val event = data["event"] as? String
                if (dateStr != null) {
                    val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                    date to AttendanceData(date, points, event)
                } else {
                    null
                }
            }.toMap()

            _attendanceData.value = parsedData

            val consecutiveAttendance = calculateConsecutiveDays(parsedData) { it.points > 0 }
            val consecutiveReading = calculateConsecutiveDays(parsedData) { it.event != null }

            _consecutiveAttendanceDays.value = consecutiveAttendance
            _consecutiveReadingDays.value = consecutiveReading

            updateUserConsecutiveDays()

            consecutiveAttendance to consecutiveReading
        } catch (e: Exception) {
            Log.e("AttendanceViewModel", "Error refreshing attendance data", e)
            0 to 0
        }
    }

    private fun calculateConsecutiveDays(
        data: Map<LocalDate, AttendanceData>,
        condition: (AttendanceData) -> Boolean
    ): Int {
        if (data.isEmpty()) return 0

        var consecutiveCount = 0
        var currentDate = LocalDate.now()

        if (data[currentDate]?.let(condition) != true) {
            currentDate = currentDate.minusDays(1)
        }

        while (data.containsKey(currentDate)) {
            val record = data[currentDate]
            if (record != null && condition(record)) {
                consecutiveCount++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }
        return consecutiveCount
    }

    private suspend fun updateUserConsecutiveDays() {
        userId ?: return
        try {
            db.collection("users").document(userId).update(
                mapOf(
                    "consecutiveDays" to _consecutiveAttendanceDays.value,
                    "consecutiveReadingDays" to _consecutiveReadingDays.value,
                    "totalReadingDays" to totalReadingDays.value
                )
            ).await()
        } catch (e: Exception) {
            Log.e("AttendanceViewModel", "Error updating user consecutive days", e)
        }
    }
}
