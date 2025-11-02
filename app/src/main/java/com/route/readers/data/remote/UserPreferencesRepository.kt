package com.route.readers.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Context의 확장 프로퍼티로 DataStore 인스턴스를 생성합니다.
// "user_preferences" 라는 이름으로 파일이 생성됩니다.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * 사용자의 환경설정(예: 다크 모드)을 DataStore를 통해 관리하는 클래스입니다.
 *
 * @param context DataStore 인스턴스를 생성하기 위해 필요한 Application Context.
 */
class UserPreferencesRepository(private val context: Context) {

    // DataStore에서 사용할 키(Key)를 정의합니다.
    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    /**
     * 현재 다크 모드 설정 상태를 Flow 형태로 제공합니다.
     * DataStore에서 값을 읽다가 에러(예: IOException)가 발생하면,
     * 비어있는 Preferences를 반환하여 앱이 크래시되지 않도록 합니다.
     */
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                // 데이터를 읽어오다 에러가 나면, 기본값(false)을 포함한 Flow를 발행합니다.
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // IS_DARK_MODE 키에 해당하는 값을 읽어옵니다. 값이 없으면 false를 기본값으로 사용합니다.
            preferences[PreferencesKeys.IS_DARK_MODE] ?: false
        }

    /**
     * 다크 모드 설정을 DataStore에 저장(업데이트)합니다.
     *
     * @param isDark 사용자가 설정한 다크 모드 활성화 여부 (true/false).
     */
    suspend fun updateDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDark
        }
    }
}
