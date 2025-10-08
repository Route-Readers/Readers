package com.route.readers.data.model

data class Challenge(
    val id: String = "",
    val title: String = "", // 예: "에세이 10권 읽기"
    val description: String = "", // 예: "두 달 동안 다양한 에세이 10권에 도전하세요!"
    val progress: Int = 0, // 현재 진행률 (예: 10권 중 3권 -> 30)
    val total: Int = 100, // 목표치 (예: 100%)
    val isCompleted: Boolean = false // 완료 여부
)
