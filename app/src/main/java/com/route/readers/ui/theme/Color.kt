package com.route.readers.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors
val PrimaryRed = Color(0xFF8B0000)
val SecondaryRed = Color(0xFFE57373)
val TertiaryRed = Color(0xFFFFAB91)
val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFFFFFFF)
val OnPrimaryLight = Color.White
val OnSecondaryLight = Color.Black
val OnTertiaryLight = Color.Black
val OnBackgroundLight = Color(0xFF1F1A1A)
val OnSurfaceLight = Color(0xFF1F1A1A)
val OnSurfaceVariantLight = Color(0xFF534343)
val SurfaceVariantLight = Color(0xFFF5F5F5)
val OutlineLight = Color(0xFF857373)
val ErrorLight = Color(0xFFB00020)
val OnErrorLight = Color.White

// Dark Theme Colors
val PrimaryRedDark = Color(0xFFE57373)
val SecondaryRedDark = Color(0xFFC62828)
val TertiaryRedDark = Color(0xFFB36B50)
val BackgroundDark = Color(0xFF1F1A1A)
val SurfaceDark = Color(0xFF2C2525)
val OnPrimaryDark = Color(0xFF4B0000)
val OnSecondaryDark = Color(0xFF4B0000)
val OnTertiaryDark = Color.White
val OnBackgroundDark = Color(0xFFF1E4E4)
val OnSurfaceDark = Color(0xFFF1E4E4)
val OnSurfaceVariantDark = Color(0xFFD7C1C1)
val SurfaceVariantDark = Color(0xFF4F4343)
val OutlineDark = Color(0xFF9F9393)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)

// Common Colors
val StarColor = Color(0xFFFFD700)
val ReadingGreen = Color(0xFF4CAF50)
val VeryLightGray = Color(0xFFFAFAFA)
val TransparentPrimary = PrimaryRed.copy(alpha = 0.1f)

// 별칭(Alias) 추가 - 다른 파일에서 DarkRed를 사용할 수 있도록 함
val DarkRed = PrimaryRed
