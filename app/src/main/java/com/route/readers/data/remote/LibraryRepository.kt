package com.route.readers.data.remote

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.gson.annotations.SerializedName
import com.route.readers.BuildConfig
import com.route.readers.ui.screens.search.LibraryApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.Locale

data class LibraryWrapper(
    @SerializedName("lib") val lib: LibraryInfo,
    @SerializedName("hasBook") val hasBook: String?
)

data class LibrarySearchResponse(
    @SerializedName("response") val response: LibsResponse
)

data class LibsResponse(
    @SerializedName("libs") val libs: List<LibraryWrapper>,
    @SerializedName("error") val error: String? = null
)

data class LibraryInfo(
    @SerializedName("libCode") val libCode: String,
    @SerializedName("libName") val libName: String,
    @SerializedName("address") val address: String,
    @SerializedName("tel") val tel: String,
    @SerializedName("homepage") val homepage: String,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?
)

data class BookAvailabilityResponse(
    @SerializedName("response") val response: AvailabilityResult
)

data class AvailabilityResult(
    @SerializedName("result") val result: BookStatus?
)

data class BookStatus(
    @SerializedName("hasBook") val hasBook: String,
    @SerializedName("loanAvailable") val loanAvailable: String
)

data class LibrarySearchResult(
    val libraryInfo: LibraryInfo,
    val isLoanAvailable: Boolean,
    val distance: Float
)

class LibraryRepository {
    private val libraryApiService: LibraryApiService = RetrofitClient.libraryApiService
    private val authKey = BuildConfig.DATA_GO_KR_API_KEY

    suspend fun getNearbyLibrariesWithBooks(
        context: Context,
        isbns: List<String>,
        userLatitude: Double,
        userLongitude: Double
    ): List<LibrarySearchResult> = withContext(Dispatchers.IO) {
        if (isbns.isEmpty()) {
            Log.w("LibraryRepository", "도서관 검색을 위한 ISBN 목록이 비어있습니다.")
            return@withContext emptyList()
        }
        if (authKey.isBlank()) {
            Log.e("LibraryRepository", "도서관 정보 나루 API 키가 비어있습니다.")
            return@withContext emptyList()
        }

        val regionCode = convertLocationToRegionCode(context, userLatitude, userLongitude)
        Log.d("LibraryRepository", "도서관 검색 시작. ISBN 개수: ${isbns.size}, 지역 코드: $regionCode")

        try {
            val librariesWithBookLists = isbns.map { isbn ->
                async { getNearbyLibrariesWithBook(regionCode, isbn, userLatitude, userLongitude) }
            }.awaitAll()

            val libraryMap = mutableMapOf<String, LibrarySearchResult>()
            val totalFound = librariesWithBookLists.sumOf { it.size }
            Log.d("LibraryRepository", "개별 ISBN으로 찾은 도서관 수 (중복 포함): $totalFound")

            librariesWithBookLists.flatten().forEach { searchResult ->
                val libCode = searchResult.libraryInfo.libCode
                val existing = libraryMap[libCode]

                if (existing == null) {
                    libraryMap[libCode] = searchResult
                } else {
                    if (searchResult.isLoanAvailable) {
                        libraryMap[libCode] = existing.copy(isLoanAvailable = true)
                    }
                }
            }
            Log.i("LibraryRepository", "도서관 검색 성공. 최종 도서관 수 (중복 제거): ${libraryMap.size}")
            return@withContext libraryMap.values.sortedBy { it.distance }
        } catch (e: Exception) {
            Log.e("LibraryRepository", "getNearbyLibrariesWithBooks (통합) 중 오류 발생", e)
            return@withContext emptyList()
        }
    }

    private suspend fun getNearbyLibrariesWithBook(
        regionCode: String,
        isbn: String,
        userLatitude: Double,
        userLongitude: Double
    ): List<LibrarySearchResult> = withContext(Dispatchers.IO) {
        try {
            val response = libraryApiService.searchLibrariesWithBook(authKey, isbn, regionCode)
            if (response.response.error != null) {
                Log.w("LibraryRepository", "API 응답 에러 (ISBN: $isbn): ${response.response.error}")
                return@withContext emptyList()
            }

            val userLocation = Location("user").apply {
                latitude = userLatitude
                longitude = userLongitude
            }

            return@withContext response.response.libs.mapNotNull { libraryItem ->
                val library = libraryItem.lib
                val libLat = library.latitude?.toDoubleOrNull()
                val libLon = library.longitude?.toDoubleOrNull()

                if (libLat != null && libLon != null) {
                    val libraryLocation = Location("library").apply {
                        latitude = libLat
                        longitude = libLon
                    }
                    val distance = userLocation.distanceTo(libraryLocation)
                    if (distance <= 20000) {
                        LibrarySearchResult(library, libraryItem.hasBook == "Y", distance)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("LibraryRepository", "getNearbyLibrariesWithBook (개별 ISBN: $isbn) 중 오류 발생", e)
            return@withContext emptyList()
        }
    }

    suspend fun getBooksAvailability(
        libCode: String,
        isbns: List<String>
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
        if (isbns.isEmpty()) return@withContext emptyMap()
        if (authKey.isBlank()) return@withContext emptyMap()

        Log.d("LibraryRepository", "대출 가능 여부 확인 시작. 도서관 코드: $libCode, ISBN 개수: ${isbns.size}")
        val availabilityMap = mutableMapOf<String, Boolean>()
        try {
            isbns.map { isbn: String ->
                async {
                    try {
                        val response = libraryApiService.getBookAvailability(authKey, libCode, isbn)
                        val isAvailable = response.response.result?.loanAvailable == "Y"
                        isbn to isAvailable
                    } catch (e: HttpException) {
                        Log.e("LibraryRepository", "대출 정보 확인 API 오류 (ISBN: $isbn, 도서관: $libCode)", e)
                        isbn to false
                    }
                }
            }.awaitAll().forEach { (isbn, isAvailable) ->
                availabilityMap[isbn] = isAvailable
            }
            Log.i("LibraryRepository", "대출 가능 여부 확인 완료. 도서관 코드: $libCode")
            return@withContext availabilityMap
        } catch (e: Exception) {
            Log.e("LibraryRepository", "getBooksAvailability 중 오류 발생. 도서관 코드: $libCode", e)
            return@withContext emptyMap()
        }
    }

    private fun convertLocationToRegionCode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String {
        val geocoder = Geocoder(context, Locale.KOREAN)
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            val adminArea = addresses?.firstOrNull()?.adminArea
            Log.d("Geocoder", "주소 변환 결과: $adminArea")

            return when {
                adminArea?.contains("서울") == true -> "11"
                adminArea?.contains("부산") == true -> "26"
                adminArea?.contains("대구") == true -> "27"
                adminArea?.contains("인천") == true -> "28"
                adminArea?.contains("광주") == true -> "29"
                adminArea?.contains("대전") == true -> "30"
                adminArea?.contains("울산") == true -> "31"
                adminArea?.contains("세종") == true -> "36"
                adminArea?.contains("경기") == true -> "41"
                adminArea?.contains("강원") == true -> "42"
                adminArea?.contains("충북") == true || adminArea?.contains("충청북도") == true -> "43"
                adminArea?.contains("충남") == true || adminArea?.contains("충청남도") == true -> "44"
                adminArea?.contains("전북") == true || adminArea?.contains("전라북도") == true -> "45"
                adminArea?.contains("전남") == true || adminArea?.contains("전라남도") == true -> "46"
                adminArea?.contains("경북") == true || adminArea?.contains("경상북도") == true -> "47"
                adminArea?.contains("경남") == true || adminArea?.contains("경상남도") == true -> "48"
                adminArea?.contains("제주") == true -> "50"
                else -> "11"
            }
        } catch (e: Exception) {
            Log.e("Geocoder", "주소 변환 중 오류 발생", e)
            return "11"
        }
    }

    suspend fun getNearbyLibraries(
        userLatitude: Double,
        userLongitude: Double
    ): List<LibrarySearchResult> = withContext(Dispatchers.IO) {
        if (authKey.isBlank()) {
            Log.e("LibraryRepository", "도서관 정보 나루 API 키가 비어있습니다.")
            return@withContext emptyList()
        }

        try {
            val response = libraryApiService.searchLibrariesByArea(
                authKey = authKey,
                latitude = userLatitude,
                longitude = userLongitude,
                pageSize = 1000
            )

            if (response.response.error != null) {
                Log.w("LibraryRepository", "API 응답 에러: ${response.response.error}")
                return@withContext emptyList()
            }

            val userLocation = Location("user").apply {
                latitude = userLatitude
                longitude = userLongitude
            }

            val results = response.response.libs.mapNotNull { libraryItem ->
                val library = libraryItem.lib
                val libLat = library.latitude?.toDoubleOrNull()
                val libLon = library.longitude?.toDoubleOrNull()

                if (libLat != null && libLon != null) {
                    val libraryLocation = Location("library").apply {
                        latitude = libLat
                        longitude = libLon
                    }
                    val distance = userLocation.distanceTo(libraryLocation)
                    LibrarySearchResult(library, false, distance)
                } else {
                    null
                }
            }
            return@withContext results.sortedBy { it.distance }.take(50)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "getNearbyLibraries 중 오류 발생", e)
            return@withContext emptyList()
        }
    }

    // 사용자 지역의 모든 도서관을 거리순으로 정렬
    suspend fun getRegionLibrariesByDistance(
        context: Context,
        userLatitude: Double,
        userLongitude: Double
    ): List<LibrarySearchResult> = withContext(Dispatchers.IO) {
        if (authKey.isBlank()) return@withContext emptyList()

        try {
            val userLocation = Location("user").apply {
                latitude = userLatitude
                longitude = userLongitude
            }

            // 사용자 위치 기준 지역 코드 가져오기
            val regionCode = convertLocationToRegionCode(context, userLatitude, userLongitude)
            
            val response = libraryApiService.searchLibrariesWithBook(
                authKey = authKey,
                isbn = "", // 빈 ISBN으로 해당 지역 모든 도서관 조회
                region = regionCode
            )
            
            if (response.response.error != null) {
                return@withContext emptyList()
            }
            
            val results = response.response.libs.mapNotNull { libraryItem ->
                val library = libraryItem.lib
                val libLat = library.latitude?.toDoubleOrNull()
                val libLon = library.longitude?.toDoubleOrNull()

                if (libLat != null && libLon != null) {
                    val libraryLocation = Location("library").apply {
                        latitude = libLat
                        longitude = libLon
                    }
                    val distance = userLocation.distanceTo(libraryLocation)
                    LibrarySearchResult(library, false, distance)
                } else null
            }

            return@withContext results.sortedBy { it.distance }
                
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
    suspend fun getAllNearbyLibraries(
        context: Context,
        userLatitude: Double,
        userLongitude: Double
    ): List<LibrarySearchResult> = withContext(Dispatchers.IO) {
        if (authKey.isBlank()) return@withContext emptyList()

        try {
            val userLocation = Location("user").apply {
                latitude = userLatitude
                longitude = userLongitude
            }

            val regionCodes = listOf("11", "41", "28", "26", "27", "29", "30", "42", "43", "44")
            
            val allLibraries = regionCodes.map { regionCode ->
                async {
                    try {
                        val response = libraryApiService.searchLibrariesWithBook(
                            authKey = authKey,
                            isbn = "",
                            region = regionCode
                        )
                        
                        response.response.libs.mapNotNull { libraryItem ->
                            val library = libraryItem.lib
                            val libLat = library.latitude?.toDoubleOrNull()
                            val libLon = library.longitude?.toDoubleOrNull()

                            if (libLat != null && libLon != null) {
                                val libraryLocation = Location("library").apply {
                                    latitude = libLat
                                    longitude = libLon
                                }
                                val distance = userLocation.distanceTo(libraryLocation)
                                if (distance <= 50000) {
                                    LibrarySearchResult(library, false, distance)
                                } else null
                            } else null
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten()

            return@withContext allLibraries
                .distinctBy { it.libraryInfo.libCode }
                .sortedBy { it.distance }
                .take(100)
                
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
}
