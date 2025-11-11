package com.route.readers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.route.readers.R
import com.route.readers.data.model.User

@Composable
fun UserProfileImage(
    user: User,
    size: Dp = 48.dp,
    fontSize: TextUnit = 20.sp
) {
    // 기본 색상을 컴포저블 컨텍스트에서 먼저 가져옵니다.
    val primaryColor = MaterialTheme.colorScheme.primary

    // remember 블록 안에서는 컴포저블 컨텍스트에 의존하지 않는 값만 사용합니다.
    val backgroundColor = remember(user.profileBackgroundColor, primaryColor) {
        user.profileBackgroundColor?.let { colorString ->
            runCatching {
                Color(android.graphics.Color.parseColor(colorString))
            }.getOrNull()
        } ?: primaryColor // 여기서 안전하게 기본 색상을 사용합니다.
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            !user.profileImageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "프로필 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            user.profileCharacter != null -> {
                val drawableRes = when (user.profileCharacter) {
                    "lion" -> R.drawable.lion
                    "penguin" -> R.drawable.penguin
                    "redpanda" -> R.drawable.redpanda
                    "squirrel" -> R.drawable.squirrel
                    "cat" -> R.drawable.cat
                    "dog" -> R.drawable.dog
                    "fox" -> R.drawable.fox
                    "rabbit" -> R.drawable.rabbit
                    else -> null
                }

                drawableRes?.let {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .build(),
                        contentDescription = "캐릭터",
                        modifier = Modifier.size(size * 0.8f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            else -> {
                Text(
                    text = user.nickname.firstOrNull()?.toString() ?: "",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
