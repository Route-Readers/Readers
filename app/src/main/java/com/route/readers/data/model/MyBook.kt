package com.route.readers.data.model

import com.google.firebase.firestore.Exclude

data class MyBook(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val author: String = "",
    val cover: String = "",
    val isbn: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val isCompleted: Boolean = false,
    val addedDate: Long = System.currentTimeMillis(),
    val lastReadDate: Long = System.currentTimeMillis(),
    val completedDate: Long? = null
) {
    @get:Exclude
    val progressPercentage: Int
        get() = if (totalPages > 0) {
            ((currentPage.toFloat() / totalPages) * 100).toInt()
        } else 0

    @Exclude
    fun getHighQualityImageUrl(): String {
        return when {
            cover.contains("aladin.co.kr") && cover.contains("/cover/") -> {
                cover.replace("/cover/", "/cover200/")
            }

            cover.contains("aladin.co.kr") && cover.contains("/cover150/") -> {
                cover.replace("/cover150/", "/cover200/")
            }

            cover.contains("aladin.co.kr") && cover.contains("/cover85/") -> {
                cover.replace("/cover85/", "/cover200/")
            }

            cover.contains("aladin.co.kr") && cover.contains("/cover75/") -> {
                cover.replace("/cover75/", "/cover200/")
            }

            else -> cover
        }
    }

    @Exclude
    fun extractPageCount(): Int {
        // Implement page extraction logic here if needed
        return totalPages
    }
}
