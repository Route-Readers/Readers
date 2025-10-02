package com.route.readers.data.model

import com.google.gson.annotations.SerializedName

data class BookListDTO(
    @SerializedName("item") val books: List<Book> = emptyList(),
    @SerializedName("totalResults") val totalResults: Int = 0,
    @SerializedName("startIndex") val startIndex: Int = 0,
    @SerializedName("itemsPerPage") val itemsPerPage: Int = 0
)
