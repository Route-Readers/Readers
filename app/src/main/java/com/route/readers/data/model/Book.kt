package com.route.readers.data.model

import com.google.firebase.firestore.Exclude
import com.google.gson.annotations.SerializedName
import android.util.Log


data class SubInfo(
    @SerializedName("itemPage") val itemPage: Int? = null
)

data class Book(
    @SerializedName("title") val title: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("isbn") val isbn: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName("itemPage") val itemPage: Int? = null,
    @SerializedName("subInfo") val subInfo: SubInfo? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("pubDate") val pubDate: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val progress: Int = 0,

    @get:Exclude
    val isFavorite: Boolean = false
) {


    fun extractPageCount(): Int {
        Log.d("Book", "subInfo: $subInfo, itemPage: $itemPage")
        return subInfo?.itemPage ?: itemPage ?: 0
    }

    fun hasValidPageInfo(): Boolean {
        return extractPageCount() > 0
    }

    fun getHighQualityImageUrl(): String {
        return cover
    }
}
