package com.route.readers.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 출석 데이터를 담을 데이터 클래스 정의
data class AttendanceData(
    val date: LocalDate,
    val points: Int = 10, // 기본 포인트
    val event: String? = null // 특별 이벤트 (예: "leaf_1", "leaf_2")
)

class AttendanceViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 출석 데이터를 저장할 StateFlow
    private val _attendanceData = MutableStateFlow<Map<LocalDate, AttendanceData>>(emptyMap())
    val attendanceData = _attendanceData.asStateFlow()

    init {
        fetchAttendanceData()
    }

    private fun fetchAttendanceData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val document = db.collection("users").document(userId).get().await()
                // Firestore에 'attendance' 필드가 List<Map<String, Any>> 형태라고 가정
                val dataFromFirestore = document["attendance"] as? List<Map<String, Any>> ?: emptyList()

                val parsedData = dataFromFirestore.mapNotNull { data ->
                    val dateStr = data["date"] as? String
                    val points = (data["points"] as? Long)?.toInt() ?: 10
                    val event = data["event"] as? String

                    if (dateStr != null) {
                        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                        date to AttendanceData(date, points, event)
                    } else {
                        null
                    }
                }.toMap()

                _attendanceData.value = parsedData
            } catch (e: Exception) {
                // 오류 처리
            }
        }
    }
}
