package com.route.readers.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 기존 DarkColorScheme과 LightColorScheme은 그대로 둡니다.
// 앱의 브랜드 색상에 맞게 이 부분을 커스텀하는 것이 좋습니다.
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    // 다크 모드에 맞는 배경색, 텍스트 색 등을 여기에 정의할 수 있습니다.
    // background = Color(0xFF121212),
    // surface = Color(0xFF121212),
    // onPrimary = Color.Black,
    // onSecondary = Color.Black,
    // onTertiary = Color.Black,
    // onBackground = Color.White,
    // onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    // 라이트 모드에 맞는 색상들을 여기에 정의할 수 있습니다.
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ReadersTheme(
    // 이 함수의 기본값을 시스템 설정 따르기에서, 외부에서 주입받는 값으로 변경합니다.
    darkTheme: Boolean,
    // Dynamic Color는 그대로 유지합니다.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 상태바와 네비게이션바의 아이콘/텍스트 색상을 darkTheme 값에 따라 자동으로 변경합니다.
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme

            // 기존의 투명 상태바, 반투명 네비게이션 바 설정은 유지합니다.
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb() // 반투명 대신 투명으로 변경하여 테마 색상과 더 잘 어울리게 할 수 있습니다.
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        // Shapes는 Material3 기본값을 사용하도록 Shapes 파라미터를 제거하거나 직접 정의한 Shapes를 전달할 수 있습니다.
        // shapes = Shapes,
        content = content
    )
}
