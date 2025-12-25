package com.route.readers.ui.screens.search

import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.remote.LibraryRepository
import com.route.readers.data.remote.LibrarySearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class NearbyLibraryUiState {
    object Idle : NearbyLibraryUiState()
    object Loading : NearbyLibraryUiState()
    data class Success(val libraries: List<LibrarySearchResult>) : NearbyLibraryUiState()
    data class Error(val message: String) : NearbyLibraryUiState()
}

class NearbyLibraryViewModel : ViewModel() {
    private val libraryRepository = LibraryRepository()

    private val _uiState = MutableStateFlow<NearbyLibraryUiState>(NearbyLibraryUiState.Idle)
    val uiState: StateFlow<NearbyLibraryUiState> = _uiState

    fun fetchNearbyLibraries(location: Location) {
        viewModelScope.launch {
            _uiState.value = NearbyLibraryUiState.Loading
            try {
                val libraries = libraryRepository.getNearbyLibraries(location.latitude, location.longitude)
                _uiState.value = NearbyLibraryUiState.Success(libraries)
            } catch (e: Exception) {
                _uiState.value = NearbyLibraryUiState.Error("주변 도서관 정보를 불러오는 데 실패했습니다: ${e.message}")
            }
        }
    }
}
