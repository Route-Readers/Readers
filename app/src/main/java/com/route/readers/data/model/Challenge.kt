package com.route.readers.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// 챌린지 종류를 명확하게 구분하기 위한 enum 클래스
enum class ChallengeType {
    CONSECUTIVE_READING_WITH_FRIEND, // 친구와 연속 읽기
    DAILY_PAGES_READING,             // 매일 페이지 읽기
    UNKNOWN                          // 알 수 없는 타입
}

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.UNKNOWN,
    val participants: List<String> = emptyList(), // 참여자 UID 목록
    val goal: Int = 0, // 챌린지 목표 (예: 7일, 30페이지)
    val progress: Map<String, Int> = emptyMap(), // 사용자별 진행 상태 (UID to progress)
    val dailyProgress: Map<String, Map<String, Int>> = emptyMap(), // 사용자별 일별 진행 (UID to date to progress)
    @ServerTimestamp val startDate: Date? = null,
    val endDate: Date? = null,
    val isCompleted: Boolean = false,
    val reward: String = "", // 보상 (예: "경험치 100XP")
    val weekNumber: Int = 0 // 주차 번호 (년도 + 주차로 구분)
)
