package com.route.readers.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.remote.LibraryRepository
import com.route.readers.data.remote.LibrarySearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LibraryUiState {
    object Idle : LibraryUiState()
    object Loading : LibraryUiState()
    data class Success(val libraries: List<LibrarySearchResult>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}

class LibraryViewModel(private val repository: LibraryRepository = LibraryRepository()) : ViewModel() {

    private val _libraryState = MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)
    val libraryState: StateFlow<LibraryUiState> = _libraryState

    fun startLoading() {
        _libraryState.value = LibraryUiState.Loading
    }

    fun searchNearbyLibrariesWithBook(
        // context는 더 이상 필요 없으므로 제거합니다.
        isbn: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            if (_libraryState.value !is LibraryUiState.Loading) {
                _libraryState.value = LibraryUiState.Loading
            }
            try {
                // Geocoder와 regionCode 없이 Repository 함수를 직접 호출합니다.
                val libraries = repository.getNearbyLibrariesWithBook(isbn, latitude, longitude)
                _libraryState.value = LibraryUiState.Success(libraries)
            } catch (e: Exception) {
                _libraryState.value = LibraryUiState.Error("책 소장 도서관 검색 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    fun searchNearbyLibraries(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            if (_libraryState.value !is LibraryUiState.Loading) {
                _libraryState.value = LibraryUiState.Loading
            }
            try {
                val libraries = repository.getNearbyLibraries(latitude, longitude)
                _libraryState.value = LibraryUiState.Success(libraries)
            } catch (e: Exception) {
                _libraryState.value = LibraryUiState.Error("주변 도서관 검색 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    fun resetState() {
        _libraryState.value = LibraryUiState.Idle
    }

    fun notifyPermissionError() {
        _libraryState.value = LibraryUiState.Error("위치 권한이 필요합니다. 설정을 확인하거나 권한을 허용해주세요.")
    }

    fun notifyLocationError() {
        _libraryState.value = LibraryUiState.Error("현재 위치를 가져오는 데 실패했습니다. 잠시 후 다시 시도해주세요.")
    }

    // Geocoder 관련 함수와 맵은 모두 삭제합니다.
}
