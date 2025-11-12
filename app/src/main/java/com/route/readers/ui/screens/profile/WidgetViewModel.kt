package com.route.readers.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.WidgetSettingsRepository
import com.route.readers.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WidgetUiState(
    val style: WidgetStyle = WidgetStyle.NORMAL,
    val showFriendReading: Boolean = true,
    val showProgressBar: Boolean = true,
    val showPlayButton: Boolean = true,
    val colorScheme: WidgetColorScheme = WidgetColorScheme.SYSTEM
)

enum class WidgetStyle {
    NORMAL,
    MINIMAL
}

enum class WidgetColorScheme {
    SYSTEM,
    LIGHT,
    DARK
}

class WidgetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WidgetSettingsRepository(application)

    private val _uiState = MutableStateFlow(
        WidgetUiState(
            style = repository.getWidgetStyle(),
            showFriendReading = repository.getShowFriendReading(),
            showProgressBar = repository.getShowProgressBar(),
            showPlayButton = repository.getShowPlayButton(),
            colorScheme = repository.getColorScheme()
        )
    )
    val uiState: StateFlow<WidgetUiState> = _uiState.asStateFlow()

    fun setWidgetStyle(style: WidgetStyle) {
        viewModelScope.launch {
            repository.setWidgetStyle(style)
            _uiState.update { it.copy(style = style) }
            WidgetUpdateHelper.updateAllWidgets()
        }
    }

    fun setShowFriendReading(show: Boolean) {
        viewModelScope.launch {
            repository.setShowFriendReading(show)
            _uiState.update { it.copy(showFriendReading = show) }
            WidgetUpdateHelper.updateAllWidgets()
        }
    }

    fun setShowProgressBar(show: Boolean) {
        viewModelScope.launch {
            repository.setShowProgressBar(show)
            _uiState.update { it.copy(showProgressBar = show) }
            WidgetUpdateHelper.updateAllWidgets()
        }
    }

    fun setShowPlayButton(show: Boolean) {
        viewModelScope.launch {
            repository.setShowPlayButton(show)
            _uiState.update { it.copy(showPlayButton = show) }
            WidgetUpdateHelper.updateAllWidgets()
        }
    }

    fun setColorScheme(scheme: WidgetColorScheme) {
        viewModelScope.launch {
            repository.setColorScheme(scheme)
            _uiState.update { it.copy(colorScheme = scheme) }
            WidgetUpdateHelper.updateAllWidgets()
        }
    }
}
