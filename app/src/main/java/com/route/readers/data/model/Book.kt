package com.route.readers.data.model

import com.google.firebase.firestore.Exclude
import com.google.gson.annotations.SerializedName

data class SubInfo(
    @SerializedName("itemPage") val itemPage: Int? = null
)

data class Book(
    @SerializedName("title") val title: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("isbn13") val isbn: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName("itemPage") val itemPage: String? = null,
    @SerializedName("subInfo") val subInfo: SubInfo? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("pubDate") val pubDate: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val progress: Int = 0,

    // isFavorite를 주 생성자로 이동하고 val로 변경
    @get:Exclude
    val isFavorite: Boolean = false
) {
    fun extractPageCount(): Int {
        return subInfo?.itemPage ?: 0
    }

    fun hasValidPageInfo(): Boolean {
        return extractPageCount() > 0
    }

    fun getHighQualityImageUrl(): String {
        // API에서 이미 Cover=Big 파라미터로 큰 이미지를 받아오므로 그대로 사용
        return cover
    }
}
