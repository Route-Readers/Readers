package com.route.readers.ui.screens.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.R
import com.google.firebase.auth.FirebaseAuth

import com.route.readers.data.model.Challenge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTopAppBar(
    consecutiveReadingDays: Int,
    tokens: Int,
    userActiveChallenge: Challenge?,
    onBlockListClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onMyAccountClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onNavigateToChallenge: () -> Unit,
    onTokenClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.mipmap.app_icon_foreground),
                    contentDescription = "Readers Logo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Readers",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            TokenBadge(
                tokens = tokens,
                onClick = onTokenClick
            )

            AttendanceBadge(
                readingDays = consecutiveReadingDays,
                onClick = onAttendanceClick
            )

            if (userActiveChallenge != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToChallenge)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Active Challenge Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = userActiveChallenge.title,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                IconButton(onClick = onNavigateToChallenge) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "챌린지",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("설정") },
                        onClick = {
                            menuExpanded = false
                            onMyAccountClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("차단 목록") },
                        onClick = {
                            menuExpanded = false
                            onBlockListClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("로그아웃") },
                        onClick = {
                            menuExpanded = false
                            FirebaseAuth.getInstance().signOut()
                            onLogoutClick()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}


@Composable
fun TokenBadge(tokens: Int, onClick: () -> Unit) {
    val tokenColor = Color(0xFFFFA000).copy(alpha = 0.4f) // 투명도 적용
    val backgroundColor = tokenColor.copy(alpha = 0.05f) // 배경도 더 투명하게

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tokenColor
        )
        Text(
            text = tokens.toString(),
            color = tokenColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
fun AttendanceBadge(readingDays: Int, onClick: () -> Unit) {
    // 0일 이상일 때 활성화된 색상, 0일일 때 비활성화된 색상을 지정합니다.
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contentColor = if (readingDays > 0) activeColor else inactiveColor
    val backgroundColor = if (readingDays > 0) activeColor.copy(alpha = 0.1f) else inactiveColor.copy(alpha = 0.1f)

    // if 조건을 제거하여 항상 보이도록 합니다.
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.book_5_24),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor // 조건에 따라 색상 변경
        )
        Text(
            text = readingDays.toString(),
            color = contentColor, // 조건에 따라 색상 변경
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
    Spacer(modifier = Modifier.width(8.dp))
}
