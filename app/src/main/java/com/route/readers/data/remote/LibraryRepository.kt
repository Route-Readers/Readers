package com.route.readers.data.remote

import android.location.Location
import android.util.Log
import com.route.readers.BuildConfig
import com.route.readers.ui.screens.search.LibraryApiService
import com.route.readers.ui.screens.search.LibraryInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// 도서관 검색 결과를 나타내는 데이터 클래스
data class LibrarySearchResult(
    val libraryInfo: LibraryInfo,
    val distance: Float, // 미터(m) 단위
    val isLoanAvailable: Boolean
)

class LibraryRepository {

    private val authKey = BuildConfig.DATA_GO_KR_API_KEY // API 인증키

    private val libraryApiService: LibraryApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://data4library.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibraryApiService::class.java)
    }

    /**
     * 특정 책(ISBN)을 가지고 있는 도서관 목록과 대출 가능 여부를 거리순으로 정렬하여 반환합니다.
     */
    suspend fun getNearbyLibrariesWithBook(
        isbn: String,
        userLatitude: Double,
        userLongitude: Double,
        region: String = "11" // 예: 서울 지역 코드
    ): List<LibrarySearchResult> {
        if (authKey.isBlank()) {
            Log.e("LibraryRepository", "API Key is missing.")
            return emptyList()
        }

        // 1. 책을 소장한 도서관 목록을 가져옵니다.
        val libraryResponse = try {
            libraryApiService.searchLibrariesByBook(authKey, isbn, region)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Failed to fetch libraries by book: ${e.message}", e)
            return emptyList()
        }

        if (!libraryResponse.isSuccessful) {
            Log.e("LibraryRepository", "API Error: ${libraryResponse.code()} - ${libraryResponse.message()}")
            return emptyList()
        }

        val libraries = libraryResponse.body()?.response?.libs ?: return emptyList()

        // 2. 각 도서관의 대출 가능 여부를 비동기적으로 확인하고, 거리를 계산합니다.
        return coroutineScope {
            val results = libraries.map { libInfo ->
                async {
                    // 대출 가능 여부 확인
                    val availabilityResponse = try {
                        libraryApiService.getBookAvailability(authKey, libInfo.libCode, isbn)
                    } catch (e: Exception) {
                        null
                    }
                    val isAvailable = availabilityResponse?.body()?.response?.result?.loanAvailable == "Y"

                    // 사용자 위치와 도서관 거리 계산
                    val distance = calculateDistance(
                        userLatitude, userLongitude,
                        libInfo.latitude.toDoubleOrNull() ?: 0.0,
                        libInfo.longitude.toDoubleOrNull() ?: 0.0
                    )

                    LibrarySearchResult(
                        libraryInfo = libInfo,
                        distance = distance,
                        isLoanAvailable = isAvailable
                    )
                }
            }.map { it.await() }

            // 3. 거리가 가까운 순으로 정렬하여 반환합니다.
            results.sortedBy { it.distance }
        }
    }

    /**
     * 두 지점 간의 거리를 미터(m) 단위로 계산합니다.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        try {
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        } catch (e: IllegalArgumentException) {
            Log.e("LibraryRepository", "Invalid location data for distance calculation.", e)
            return Float.MAX_VALUE
        }
        return results[0]
    }
}
