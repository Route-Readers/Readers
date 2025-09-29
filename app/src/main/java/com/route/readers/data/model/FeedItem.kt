package com.route.readers.data.model

data class FeedItem(
    val user: User,
    val book: Book,
    val currentPage: Int,
    val progressPercent: Int,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)