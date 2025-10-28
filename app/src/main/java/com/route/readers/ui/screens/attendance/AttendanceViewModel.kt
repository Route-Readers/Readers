package com.route.readers.ui.screens.attendance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AttendanceData(
    val date: LocalDate,
    val points: Int = 10,
    val event: String? = null,
    val consecutiveDays: Int = 1 // 연속 출석일 필드 추가
)

class AttendanceViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _attendanceData = MutableStateFlow<Map<LocalDate, AttendanceData>>(emptyMap())
    val attendanceData = _attendanceData.asStateFlow()

    private var isCheckingAttendance = false

    init {
        refreshAttendanceData()
    }

    fun checkAttendance() {
        if (isCheckingAttendance) return // 중복 실행 방지
        viewModelScope.launch {
            isCheckingAttendance = true
            val userId = auth.currentUser?.uid ?: run {
                isCheckingAttendance = false
                return@launch
            }
            val today = LocalDate.now()

            // 오늘 날짜 출석 기록이 이미 있는지 확인
            if (_attendanceData.value.containsKey(today)) {
                isCheckingAttendance = false
                return@launch
            }

            try {
                // 어제 날짜의 연속 출석일 가져오기
                val yesterday = today.minusDays(1)
                val lastConsecutiveDays = _attendanceData.value[yesterday]?.consecutiveDays ?: 0
                val newConsecutiveDays = lastConsecutiveDays + 1

                val newAttendanceRecord = mapOf(
                    "date" to today.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "points" to 10,
                    "event" to null,
                    "consecutiveDays" to newConsecutiveDays
                )

                // Firestore에 새로운 출석 기록 추가
                db.collection("users").document(userId)
                    .update("attendance", FieldValue.arrayUnion(newAttendanceRecord))
                    .await()

                // StateFlow를 즉시 업데이트하여 UI에 반영
                val newAttendanceData = AttendanceData(date = today, consecutiveDays = newConsecutiveDays)
                _attendanceData.update { currentMap ->
                    currentMap + (today to newAttendanceData)
                }
                Log.d("AttendanceViewModel", "Attendance checked for today. Consecutive days: $newConsecutiveDays")

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
                    val points = (data["points"] as? Long)?.toInt() ?: 10
                    val event = data["event"] as? String
                    val consecutiveDays = (data["consecutiveDays"] as? Long)?.toInt() ?: 1

                    if (dateStr != null) {
                        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                        date to AttendanceData(date, points, event, consecutiveDays)
                    } else {
                        null
                    }
                }.toMap()

                _attendanceData.value = parsedData
                Log.d("AttendanceViewModel", "Attendance data refreshed. Total records: ${parsedData.size}")
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error refreshing attendance data", e)
            }
        }
    }
}
