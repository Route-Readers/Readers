package com.route.readers.data.remote

import android.location.Location
import com.route.readers.BuildConfig
import com.route.readers.ui.screens.search.LibraryApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

data class LibraryWrapper(val lib: LibraryInfo)

data class LibrarySearchResponse(val response: LibsResponse?)
data class LibsResponse(val libs: List<LibraryWrapper>?) // LibraryInfo -> LibraryWrapper로 변경
data class LibraryInfo(
    val libCode: String,
    val libName: String,
    val address: String,
    val tel: String,
    val homepage: String,
    val latitude: String?,
    val longitude: String?
)
data class BookAvailabilityResponse(val response: AvailabilityResult?)
data class AvailabilityResult(val result: BookStatus?)
data class BookStatus(val hasBook: String, val loanAvailable: String)

data class LibrarySearchResult(
    val libraryInfo: LibraryInfo,
    val isLoanAvailable: Boolean,
    val distance: Float
)

class LibraryRepository {
    private val libraryApiService: LibraryApiService = RetrofitClient.libraryApiService
    private val authKey = BuildConfig.DATA_GO_KR_API_KEY

    suspend fun getNearbyLibrariesWithBook(
        isbn: String,
        userLatitude: Double,
        userLongitude: Double
    ): List<LibrarySearchResult> = withContext(Dispatchers.IO) {

        val nearbyLibraries = getNearbyLibraries(userLatitude, userLongitude)

        val librariesWithBook = nearbyLibraries.map { libraryResult ->
            async {
                try {
                    val availabilityResponse = libraryApiService.getBookAvailability(
                        authKey = authKey,
                        libCode = libraryResult.libraryInfo.libCode,
                        isbn13 = isbn
                    )
                    if (availabilityResponse.isSuccessful && availabilityResponse.body()?.response?.result?.hasBook == "Y") {
                        val isLoanAvailable = availabilityResponse.body()?.response?.result?.loanAvailable == "Y"
                        // 책을 보유한 도서관만 isLoanAvailable 값을 업데이트하여 반환
                        libraryResult.copy(isLoanAvailable = isLoanAvailable)
                    } else {
                        // 책이 없으면 null을 반환하여 최종 목록에서 제외
                        null
                    }
                } catch (e: Exception) {
                    // API 호출 중 에러 발생 시 제외
                    null
                }
            }
        }.awaitAll().filterNotNull() // null이 아닌 결과만 필터링

        // 최종적으로 책을 보유한 도서관 목록을 거리순으로 정렬하여 반환
        return@withContext librariesWithBook.sortedBy { it.distance }
    }

    suspend fun getNearbyLibraries(
        userLatitude: Double,
        userLongitude: Double
    ): List<LibrarySearchResult> = withContext(Dispatchers.IO) {
        val searchResponse = libraryApiService.searchLibrariesByArea(
            authKey = authKey,
            latitude = userLatitude,
            longitude = userLongitude,
            radius = 50 // 반경 50km
        )

        if (!searchResponse.isSuccessful || searchResponse.body()?.response?.libs == null) {
            return@withContext emptyList()
        }

        val libraries = searchResponse.body()!!.response!!.libs!!
            .mapNotNull { libraryWrapper ->
                val library = libraryWrapper.lib // 실제 도서관 정보 추출
                val libLat = library.latitude?.toDoubleOrNull()
                val libLon = library.longitude?.toDoubleOrNull()

                if (libLat != null && libLon != null) {
                    val distanceArray = FloatArray(1)
                    Location.distanceBetween(userLatitude, userLongitude, libLat, libLon, distanceArray)
                    LibrarySearchResult(
                        libraryInfo = library,
                        isLoanAvailable = false, // 기본값은 false, 책 검색 시 업데이트됨
                        distance = distanceArray[0]
                    )
                } else {
                    null
                }
            }
        return@withContext libraries.sortedBy { it.distance }
    }
}
