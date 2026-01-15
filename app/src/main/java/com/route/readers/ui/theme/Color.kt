package com.route.readers.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors
val PrimaryRed = Color(0xFF800020) // Muted Burgundy
val SecondaryRed = Color(0xFF8D6E63) // Warm Brownish Secondary
val TertiaryRed = Color(0xFFBCAAA4)
val BackgroundLight = Color(0xFFFFFFFF) // Pure White
val SurfaceLight = Color(0xFFFFFFFF)
val OnPrimaryLight = Color.White
val OnSecondaryLight = Color.White
val OnTertiaryLight = Color.Black
val OnBackgroundLight = Color(0xFF1A1A1A) // Deep Charcoal
val OnSurfaceLight = Color(0xFF1A1A1A)
val OnSurfaceVariantLight = Color(0xFF4E4545)
val SurfaceVariantLight = Color(0xFFF8F8F8) // Very Light Gray for depth
val OutlineLight = Color(0xFF857373)
val ErrorLight = Color(0xFFB00020)
val OnErrorLight = Color.White

// Dark Theme Colors
val PrimaryRedDark = Color(0xFFE57373) // Keep readable on dark
val SecondaryRedDark = Color(0xFFD7CCC8)
val TertiaryRedDark = Color(0xFFBCAAA4)
val BackgroundDark = Color(0xFF1A1A1A) // Deep Charcoal
val SurfaceDark = Color(0xFF242020)
val OnPrimaryDark = Color(0xFF4B0000)
val OnSecondaryDark = Color(0xFF4B0000)
val OnTertiaryDark = Color.White
val OnBackgroundDark = Color(0xFFEBE0D0) // Warm off-white text
val OnSurfaceDark = Color(0xFFEBE0D0)
val OnSurfaceVariantDark = Color(0xFFD7C1C1)
val SurfaceVariantDark = Color(0xFF4F4343)
val OutlineDark = Color(0xFF9F9393)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)

// Common Colors
val StarColor = Color(0xFFFFD700)
val ReadingGreen = Color(0xFF4CAF50)
val VeryLightGray = Color(0xFFF8F8F8) // Updated to Very Light Gray
val TransparentPrimary = PrimaryRed.copy(alpha = 0.1f)

// 별칭(Alias) 추가 - 다른 파일에서 DarkRed를 사용할 수 있도록 함
val DarkRed = PrimaryRed
