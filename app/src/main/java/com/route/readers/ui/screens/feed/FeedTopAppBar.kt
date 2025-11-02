package com.route.readers.ui.screens.feedimport

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.R
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTopAppBar(
    consecutiveReadingDays: Int,
    onBlockListClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onMyAccountClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onNavigateToChallenge: () -> Unit
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
            AttendanceBadge(
                readingDays = consecutiveReadingDays,
                onClick = onAttendanceClick
            )

            IconButton(onClick = onNavigateToChallenge) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "챌린지",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        text = { Text("내 계정") },
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
fun AttendanceBadge(readingDays: Int, onClick: () -> Unit) {
    if (readingDays > 0) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(CircleShape)
                .background(DarkRed.copy(alpha = 0.1f))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.book_5_24),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = DarkRed
            )
            Text(
                text = readingDays.toString(),
                color = DarkRed,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}
