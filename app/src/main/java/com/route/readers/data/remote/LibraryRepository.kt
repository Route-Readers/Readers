package com.route.readers.data.remote

import android.location.Location
import android.util.Log
import com.route.readers.BuildConfig
import com.route.readers.ui.screens.search.LibraryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

data class LibrarySearchResult(
    val libraryInfo: LibraryInfo,
    val distance: Float, // 미터(m) 단위
    val isLoanAvailable: Boolean
)

class LibraryRepository {

    private val authKey = BuildConfig.DATA_GO_KR_API_KEY
    private val apiService = RetrofitClient.libraryApiService

    // ▼▼▼ 여기에 전국 모든 지역 코드를 추가했습니다! ▼▼▼
    private val regions = listOf(
        "11", // 서울
        "21", // 부산
        "22", // 대구
        "23", // 인천
        "24", // 광주
        "25", // 대전
        "26", // 울산
        "29", // 세종
        "31", // 경기
        "32", // 강원
        "33", // 충북
        "34", // 충남
        "35", // 전북
        "36", // 전남
        "37", // 경북
        "38", // 경남
        "39"  // 제주
    )

    suspend fun getNearbyLibrariesWithBook(
        isbn: String,
        userLatitude: Double,
        userLongitude: Double
    ): List<LibrarySearchResult> = withContext(Dispatchers.IO) {
        if (authKey.isBlank()) {
            Log.e("LibraryRepository", "API Key is missing.")
            return@withContext emptyList()
        }

        try {
            // 여러 지역을 병렬로 검색하여 결과를 모두 합칩니다.
            val allLibraries = regions.map { regionCode ->
                async {
                    try {
                        val response = apiService.searchLibrariesByBook(authKey, isbn, regionCode)
                        if (response.isSuccessful) {
                            response.body()?.response?.libs ?: emptyList()
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten().distinctBy { it.libCode } // 중복 제거

            if (allLibraries.isEmpty()) {
                Log.w("LibraryRepository", "No libraries found for ISBN: $isbn in any monitored region.")
                return@withContext emptyList()
            }

            // 각 도서관의 대출 가능 여부와 거리를 계산합니다.
            val results = allLibraries.map { libInfo ->
                async {
                    val isAvailable = getBookLoanAvailability(libInfo.libCode, isbn)
                    val distance = calculateDistance(
                        userLatitude, userLongitude,
                        libInfo.latitude.toDoubleOrNull() ?: 0.0,
                        libInfo.longitude.toDoubleOrNull() ?: 0.0
                    )
                    LibrarySearchResult(libInfo, distance, isAvailable)
                }
            }.awaitAll()

            // 유효한 거리의 도서관만 필터링하여 거리순으로 정렬합니다.
            return@withContext results.filter { it.distance != Float.MAX_VALUE }.sortedBy { it.distance }

        } catch (e: Exception) {
            Log.e("LibraryRepository", "Failed to get nearby libraries: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    // getBookLoanAvailability, calculateDistance 함수는 기존과 동일하게 유지
    // ... (이하 코드는 변경 없음) ...
    private suspend fun getBookLoanAvailability(libCode: String, isbn: String): Boolean {
        return try {
            val response = apiService.getBookAvailability(authKey, libCode, isbn)
            if (response.isSuccessful) {
                val result = response.body()?.response?.result
                result?.hasBook == "Y" && result.loanAvailable == "Y"
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        if (lat1 == 0.0 || lon1 == 0.0 || lat2 == 0.0 || lon2 == 0.0) {
            return Float.MAX_VALUE
        }
        val results = FloatArray(1)
        return try {
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            results[0]
        } catch (e: IllegalArgumentException) {
            Log.e("LibraryRepository", "Invalid location data for distance calculation.", e)
            Float.MAX_VALUE
        }
    }
}
