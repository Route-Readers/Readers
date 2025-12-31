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
    @SerializedName("isbn") val isbn: String = "",
    @SerializedName("isbn13") val isbn13: String? = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName("itemPage") val itemPage: Int? = null,
    @SerializedName("subInfo") val subInfo: SubInfo? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("pubDate") val pubDate: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val progress: Int = 0,
    val isCompleted: Boolean = false,
    @get:Exclude
    val isFavorite: Boolean = false
) {
    val genre: String
        get() {
            val categories = categoryName?.split(">")?.map { it.trim() }?.filter { it.isNotEmpty() }
            if (categories.isNullOrEmpty()) {
                return "기타"
            }

            val relevantCategories = if (categories.firstOrNull() == "국내도서") {
                categories.drop(1)
            } else {
                categories
            }

            val genreString = relevantCategories.take(2).joinToString(" > ")
            return if (genreString.isBlank()) "기타" else genreString
        }

    fun extractPageCount(): Int {
        return subInfo?.itemPage ?: itemPage ?: 0
    }

    fun hasValidPageInfo(): Boolean {
        return extractPageCount() > 0
    }

    fun getHighQualityImageUrl(): String {
        if (cover.isBlank()) {
            return ""
        }
        return cover.replace("/tcover", "/cover500")
    }
}
