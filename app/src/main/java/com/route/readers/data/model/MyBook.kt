package com.route.readers.data.model

data class MyBook(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val cover: String = "",
    val isbn: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val addedDate: Long = System.currentTimeMillis(),
    val lastReadDate: Long = System.currentTimeMillis()
) {
    val progressPercentage: Int
        get() = if (totalPages > 0) {
            ((currentPage.toFloat() / totalPages) * 100).toInt()
        } else 0

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
}
