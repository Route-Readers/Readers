package com.route.readers.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.remote.LibraryRepository
import com.route.readers.data.remote.LibrarySearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 도서관 검색 UI 상태를 나타내는 클래스
sealed class LibraryUiState {
    object Idle : LibraryUiState() // 초기 상태
    object Loading : LibraryUiState() // 검색 중
    data class Success(val libraries: List<LibrarySearchResult>) : LibraryUiState() // 성공
    data class Error(val message: String) : LibraryUiState() // 오류
}

class LibraryViewModel : ViewModel() {

    private val repository = LibraryRepository()

    private val _libraryState = MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)
    val libraryState = _libraryState.asStateFlow()

    /**
     * 주변 도서관 검색을 시작합니다.
     * @param isbn 검색할 책의 ISBN
     * @param latitude 사용자 현재 위도
     * @param longitude 사용자 현재 경도
     */
    fun searchNearbyLibraries(isbn: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _libraryState.value = LibraryUiState.Loading
            try {
                val results = repository.getNearbyLibrariesWithBook(isbn, latitude, longitude)
                if (results.isNotEmpty()) {
                    _libraryState.value = LibraryUiState.Success(results)
                } else {
                    _libraryState.value = LibraryUiState.Error("주변에 해당 책을 소장한 도서관이 없거나, 검색 중 오류가 발생했습니다.")
                }
            } catch (e: Exception) {
                _libraryState.value = LibraryUiState.Error("오류 발생: ${e.message}")
            }
        }
    }

    // UI 상태를 초기화하는 함수
    fun resetState() {
        _libraryState.value = LibraryUiState.Idle
    }
}
