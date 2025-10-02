package com.route.readers.data.model

import com.google.gson.annotations.SerializedName

data class Book(
    @SerializedName("title") val title: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("isbn13") val isbn: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName("itemPage") val itemPage: String? = null,
    // 독서 진행 상황 관련 필드들 (로컬 데이터)
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val progress: Int = 0
)
