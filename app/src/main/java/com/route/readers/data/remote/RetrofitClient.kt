package com.route.readers.data.remote

import com.route.readers.ui.screens.search.LibraryApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val LIBRARY_API_BASE_URL = "http://data4library.kr/"

    private val libraryRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(LIBRARY_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val libraryApiService: LibraryApiService by lazy {
        libraryRetrofit.create(LibraryApiService::class.java)
    }

    // 나중에 다른 API(e.g., Aladin)를 추가할 경우 여기에 정의
    // private const val ALADIN_API_BASE_URL = "..."
    // val aladinApiService: AladinApiService by lazy { ... }
}
