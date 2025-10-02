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
        get() = if (totalPages > 0) (currentPage * 100 / totalPages) else 0
}
