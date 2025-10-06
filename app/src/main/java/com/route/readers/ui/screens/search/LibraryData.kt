package com.route.readers.ui.screens.search

import com.google.gson.annotations.SerializedName

data class LibrarySearchResponse(
    val response: LibsResponse?
)

data class LibsResponse(
    @SerializedName("lib")
    val libs: List<LibraryInfo>?
)

data class LibraryInfo(
    @SerializedName("libCode") val libCode: String,
    @SerializedName("libName") val libName: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("homepage") val homepage: String? = null,
    @SerializedName("tel") val tel: String? = null
)

data class BookAvailabilityResponse(
    val response: AvailabilityResult?
)

data class AvailabilityResult(
    val result: BookStatus?
)

data class BookStatus(
    @SerializedName("hasBook") val hasBook: String,
    @SerializedName("loanAvailable") val loanAvailable: String
)
