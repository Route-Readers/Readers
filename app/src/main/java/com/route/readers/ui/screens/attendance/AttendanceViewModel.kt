package com.route.readers.ui.screens.attendance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    // --- 여기부터 새로운 함수 및 수정된 코드 ---

    /**
     * '내 서재'에서 책 진도율 업데이트 시 호출될 함수입니다.
     * 오늘 날짜에 책 읽기 활동(event)을 기록합니다.
     */
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

                // 1. 이미 오늘 출석/활동 기록이 있는 경우
                if (todayRecordIndex != -1) {
                    // event 필드만 "leaf_1"으로 업데이트
                    val updatedList = attendanceList.toMutableList()
                    val todayData = updatedList[todayRecordIndex].toMutableMap()
                    todayData["event"] = "leaf_1" // 책 읽기 이벤트로 변경
                    updatedList[todayRecordIndex] = todayData as HashMap<String, Any>

                    userDocRef.update("attendance", updatedList).await()
                    Log.d("AttendanceViewModel", "Updated today's attendance with reading event.")
                }
                // 2. 오늘 첫 활동 기록인 경우
                else {
                    // 출석은 하지 않았으므로 consecutiveDays는 0으로 시작
                    val newReadingRecord = hashMapOf(
                        "date" to todayStr,
                        "points" to 0, // 책 읽기는 포인트를 주지 않는다고 가정
                        "event" to "leaf_1", // 책 읽기 이벤트
                        "consecutiveDays" to 0 // 출석이 아니므로 연속 출석일은 0
                    )
                    userDocRef.update("attendance", FieldValue.arrayUnion(newReadingRecord)).await()
                    Log.d("AttendanceViewModel", "Added new reading event for today.")
                }

                // UI 즉시 업데이트
                refreshAttendanceData()

            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error marking reading activity", e)
            }
        }
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
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // 오늘 날짜 출석 기록이 이미 있는지 확인 (event가 null인 경우만 출석으로 간주)
            val todayData = _attendanceData.value[today]
            if (todayData != null && todayData.consecutiveDays > 0) {
                Log.d("AttendanceViewModel", "Already checked attendance today.")
                isCheckingAttendance = false
                return@launch
            }

            try {
                val userDocRef = db.collection("users").document(userId)

                // 어제 날짜의 연속 출석일 가져오기
                val yesterday = today.minusDays(1)
                val lastConsecutiveDays = _attendanceData.value[yesterday]?.consecutiveDays ?: 0
                val newConsecutiveDays = lastConsecutiveDays + 1

                // Firestore에서 오늘 날짜의 기록을 직접 찾아서 업데이트 또는 추가
                val attendanceList = (_attendanceData.value.values.map {
                    // AttendanceData를 Firestore에 저장된 형태(HashMap)로 변환
                    hashMapOf(
                        "date" to it.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "points" to it.points,
                        "event" to it.event,
                        "consecutiveDays" to it.consecutiveDays
                    )
                }).toMutableList()

                val todayRecordIndex = attendanceList.indexOfFirst { it["date"] == todayStr }

                // 1. 이미 책읽기(event) 기록이 있는 경우 -> 해당 기록에 출석 정보(연속일, 포인트)를 덮어씀
                if (todayRecordIndex != -1) {
                    val recordToUpdate = attendanceList[todayRecordIndex]
                    recordToUpdate["points"] = 10
                    recordToUpdate["consecutiveDays"] = newConsecutiveDays
                    // event는 그대로 유지
                    attendanceList[todayRecordIndex] = recordToUpdate
                    userDocRef.update("attendance", attendanceList).await()
                    Log.d("AttendanceViewModel", "Updated reading event with attendance info.")
                }
                // 2. 오늘 첫 활동이 출석인 경우 -> 새로운 출석 기록 추가
                else {
                    val newAttendanceRecord = hashMapOf(
                        "date" to todayStr,
                        "points" to 10,
                        "event" to null, // 일반 출석은 event가 null
                        "consecutiveDays" to newConsecutiveDays
                    )
                    userDocRef.update("attendance", FieldValue.arrayUnion(newAttendanceRecord)).await()
                    Log.d("AttendanceViewModel", "Attendance checked for today. Consecutive days: $newConsecutiveDays")
                }

                // UI 즉시 업데이트
                refreshAttendanceData()

            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error checking attendance", e)
            } finally {
                isCheckingAttendance = false
            }
        }
    }
    // --- refreshAttendanceData() 함수는 기존 코드와 동일 ---

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
                    val consecutiveDays = (data["consecutiveDays"] as? Long)?.toInt() ?: 0 // 기본값을 0으로 변경

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
