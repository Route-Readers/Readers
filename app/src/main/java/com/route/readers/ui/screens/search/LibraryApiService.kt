package com.route.readers.ui.screens.search

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 도서관 정보나루 API와 통신하기 위한 Retrofit 인터페이스
 */
interface LibraryApiService {

    @GET("api/libSrchByBook")
    suspend fun searchLibrariesByBook(
        @Query("authKey") authKey: String,
        @Query("isbn") isbn: String,
        @Query("region") region: String,
        @Query("format") format: String = "json"
    ): Response<LibrarySearchResponse>

    @GET("api/bookExist")
    suspend fun getBookAvailability(
        @Query("authKey") authKey: String,
        @Query("libCode") libCode: String,
        @Query("isbn13") isbn13: String,
        @Query("format") format: String = "json"
    ): Response<BookAvailabilityResponse>
}
