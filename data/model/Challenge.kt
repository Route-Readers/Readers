package com.route.readers.data.model

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val participants: List<String> = emptyList(),
    val goal: Int = 0, // e.g., number of pages or books
    val progress: Map<String, Int> = emptyMap(), // userId to progress
    val type: ChallengeType = ChallengeType.SOLO // SOLO or GROUP
)

enum class ChallengeType {
    SOLO,
    GROUP
}
