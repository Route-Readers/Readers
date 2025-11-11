package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.ui.screens.attendance.AttendanceData
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AttendanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid

    suspend fun getAttendanceData(): Map<LocalDate, AttendanceData> {
        userId ?: return emptyMap()
        return try {
            val document = db.collection("users").document(userId).get().await()
            val dataFromFirestore =
                document["attendance"] as? List<Map<String, Any>> ?: emptyList()

            dataFromFirestore.mapNotNull { data ->
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
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error getting attendance data", e)
            emptyMap()
        }
    }

    fun calculateConsecutiveDays(
        data: Map<LocalDate, AttendanceData>,
        condition: (AttendanceData) -> Boolean
    ): Int {
        if (data.isEmpty()) return 0

        var consecutiveCount = 0
        var currentDate = LocalDate.now()

        // 오늘부터 시작해서 연속된 날들을 카운트
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
}