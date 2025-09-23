package com.route.readers.data.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverImage: String = "",
    val totalPages: Int = 0
)
