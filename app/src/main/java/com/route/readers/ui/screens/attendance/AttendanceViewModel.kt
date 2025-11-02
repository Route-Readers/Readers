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
import java.util.SortedMap

data class AttendanceData(
    val date: LocalDate,
    val points: Int = 10,
    val event: String? = null,
    val consecutiveDays: Int = 1
)

class AttendanceViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _attendanceData = MutableStateFlow<Map<LocalDate, AttendanceData>>(emptyMap())
    val attendanceData = _attendanceData.asStateFlow()

    // --- 새로운 StateFlow 추가 ---
    private val _consecutiveReadingDays = MutableStateFlow(0)
    val consecutiveReadingDays: StateFlow<Int> = _consecutiveReadingDays.asStateFlow()
    // ---------------------------

    val totalAttendanceDays: StateFlow<Int> = attendanceData.map { dataMap ->
        dataMap.values.count { it.points > 0 } // 출석은 포인트가 있는 날로 계산
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- 이름 변경으로 가독성 향상 ---
    val totalReadingDays: StateFlow<Int> = attendanceData.map { dataMap ->
        dataMap.values.count { it.event != null } // 독서 기록은 event가 있는 날로 계산
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    // ---------------------------------

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
                    todayData["event"] = "leaf_1" // 간단하게 leaf_1로 통일 (혹은 다른 로직)
                    updatedList[todayRecordIndex] = todayData as HashMap<String, Any>

                    userDocRef.update("attendance", updatedList).await()
                } else {
                    // 어제 날짜의 연속 출석일 가져오기 (독서 기록 시 출석도 같이 처리될 수 있도록)
                    val yesterday = today.minusDays(1)
                    val lastConsecutiveDays = _attendanceData.value[yesterday]?.consecutiveDays ?: 0
                    // 독서 기록은 연속일수에 직접 영향을 주지 않고, 출석 시에만 계산
                    val newReadingRecord = hashMapOf(
                        "date" to todayStr,
                        "points" to 0,
                        "event" to "leaf_1",
                        "consecutiveDays" to lastConsecutiveDays // 출석 전이므로 이전 값을 유지
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
            if (todayData != null && todayData.points > 0) { // 이미 출석(포인트 획득)했으면 중단
                isCheckingAttendance = false
                return@launch
            }

            try {
                val userDocRef = db.collection("users").document(userId)

                val yesterday = today.minusDays(1)
                val lastConsecutiveDays = _attendanceData.value[yesterday]?.consecutiveDays ?: 0
                val newConsecutiveDays = lastConsecutiveDays + 1

                val document = userDocRef.get().await()
                val attendanceList = (document["attendance"] as? List<HashMap<String, Any>>)?.toMutableList() ?: mutableListOf()

                val todayRecordIndex = attendanceList.indexOfFirst { it["date"] == todayStr }

                if (todayRecordIndex != -1) {
                    val recordToUpdate = attendanceList[todayRecordIndex]
                    recordToUpdate["points"] = 10
                    recordToUpdate["consecutiveDays"] = newConsecutiveDays
                    attendanceList[todayRecordIndex] = recordToUpdate
                    userDocRef.update("attendance", attendanceList).await()
                } else {
                    val newAttendanceRecord = hashMapOf(
                        "date" to todayStr,
                        "points" to 10,
                        "event" to null,
                        "consecutiveDays" to newConsecutiveDays
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
                    val points = (data["points"] as? Long)?.toInt() ?: 0 // 기본값 0으로 변경
                    val event = data["event"] as? String
                    val consecutiveDays = (data["consecutiveDays"] as? Long)?.toInt() ?: 0

                    if (dateStr != null) {
                        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                        date to AttendanceData(date, points, event, consecutiveDays)
                    } else {
                        null
                    }
                }.toMap()

                _attendanceData.value = parsedData
                // --- 연속 독서일 계산 로직 추가 ---
                calculateConsecutiveReadingDays(parsedData)
                // ---------------------------------

            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error refreshing attendance data", e)
            }
        }
    }

    // --- 연속 독서일 계산 함수 추가 ---
    private fun calculateConsecutiveReadingDays(data: Map<LocalDate, AttendanceData>) {
        if (data.isEmpty()) {
            _consecutiveReadingDays.value = 0
            return
        }

        // 날짜를 기준으로 오름차순 정렬
        val sortedData: SortedMap<LocalDate, AttendanceData> = data.toSortedMap()
        var currentDate = LocalDate.now()
        var consecutiveCount = 0

        // 오늘부터 과거로 거슬러 올라가며 연속 독서일 계산
        while (true) {
            val record = sortedData[currentDate]
            if (record != null && record.event != null) { // 독서 기록(event)이 있는지 확인
                consecutiveCount++
                currentDate = currentDate.minusDays(1) // 하루 전으로 이동
            } else {
                break // 연속이 끊기면 종료
            }
        }
        _consecutiveReadingDays.value = consecutiveCount
    }
    // ------------------------------------
}
