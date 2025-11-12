package com.route.readers.data

import android.content.Context
import android.content.SharedPreferences
import com.route.readers.ui.screens.profile.WidgetColorScheme
import com.route.readers.ui.screens.profile.WidgetStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WidgetSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)

    private val _widgetStyle = MutableStateFlow(getWidgetStyle())
    val widgetStyle: StateFlow<WidgetStyle> = _widgetStyle

    private val _showFriendReading = MutableStateFlow(getShowFriendReading())
    val showFriendReading: StateFlow<Boolean> = _showFriendReading

    private val _showProgressBar = MutableStateFlow(getShowProgressBar())
    val showProgressBar: StateFlow<Boolean> = _showProgressBar

    private val _colorScheme = MutableStateFlow(getColorScheme())
    val colorScheme: StateFlow<WidgetColorScheme> = _colorScheme

    fun getWidgetStyle(): WidgetStyle {
        return WidgetStyle.valueOf(prefs.getString("style", WidgetStyle.NORMAL.name) ?: WidgetStyle.NORMAL.name)
    }

    fun setWidgetStyle(style: WidgetStyle) {
        prefs.edit().putString("style", style.name).apply()
        _widgetStyle.value = style
    }

    fun getShowFriendReading(): Boolean {
        return prefs.getBoolean("showFriendReading", true)
    }

    fun setShowFriendReading(show: Boolean) {
        prefs.edit().putBoolean("showFriendReading", show).apply()
        _showFriendReading.value = show
    }

    fun getShowProgressBar(): Boolean {
        return prefs.getBoolean("showProgressBar", true)
    }

    fun setShowProgressBar(show: Boolean) {
        prefs.edit().putBoolean("showProgressBar", show).apply()
        _showProgressBar.value = show
    }

    fun getColorScheme(): WidgetColorScheme {
        return WidgetColorScheme.valueOf(
            prefs.getString("colorScheme", WidgetColorScheme.SYSTEM.name) ?: WidgetColorScheme.SYSTEM.name
        )
    }

    fun setColorScheme(scheme: WidgetColorScheme) {
        prefs.edit().putString("colorScheme", scheme.name).apply()
        _colorScheme.value = scheme
    }

    fun getShowPlayButton(): Boolean {
        return prefs.getBoolean("showPlayButton", true)
    }

    fun setShowPlayButton(show: Boolean) {
        prefs.edit().putBoolean("showPlayButton", show).apply()
    }
}
