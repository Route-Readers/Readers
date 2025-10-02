package com.route.readers.data.model

import com.google.gson.annotations.SerializedName

data class BookListDTO(
    @SerializedName("item") val books: List<Book>
)
