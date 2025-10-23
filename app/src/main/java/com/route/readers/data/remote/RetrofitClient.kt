package com.route.readers.data.remote

import com.route.readers.ui.screens.search.LibraryApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val LIBRARY_API_BASE_URL = "http://data4library.kr/"

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val libraryRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(LIBRARY_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val libraryApiService: LibraryApiService by lazy {
        libraryRetrofit.create(LibraryApiService::class.java)
    }
}
