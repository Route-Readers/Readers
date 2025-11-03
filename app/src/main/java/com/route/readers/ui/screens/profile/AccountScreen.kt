package com.route.readers.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.ui.theme.DarkRed

enum class MenuItemType {
    PRIVACY,
    ACTIVITY,
    STATISTICS,
    DISPLAY,
    TERMS,
    CONTACT
}

data class AccountMenuItem(
    val type: MenuItemType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    viewModel: AccountViewModel = viewModel()
) {
    val context = LocalContext.current
    var isPrivacyMenuExpanded by remember { mutableStateOf(false) }
    var isActivityMenuExpanded by remember { mutableStateOf(false) }
    var isStatisticsMenuExpanded by remember { mutableStateOf(false) }
    var isDisplayMenuExpanded by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val onDarkModeChange: (Boolean) -> Unit = { newDarkModeState ->
        viewModel.updateDarkModeSetting(newDarkModeState)
    }

    val menuItems = listOf(
        AccountMenuItem(
            type = MenuItemType.PRIVACY,
            title = "공개 범위",
            subtitle = "프로필 및 활동 공개 설정",
            icon = Icons.Default.Lock,
            onClick = {
                isPrivacyMenuExpanded = !isPrivacyMenuExpanded
                isActivityMenuExpanded = false
                isStatisticsMenuExpanded = false
                isDisplayMenuExpanded = false
            }
        ),
        AccountMenuItem(
            type = MenuItemType.ACTIVITY,
            title = "내 활동",
            subtitle = "내가 남긴 기록 확인하기",
            icon = Icons.Default.History,
            onClick = {
                isActivityMenuExpanded = !isActivityMenuExpanded
                isPrivacyMenuExpanded = false
                isStatisticsMenuExpanded = false
                isDisplayMenuExpanded = false
            }
        ),
        AccountMenuItem(
            type = MenuItemType.STATISTICS,
            title = "통계",
            subtitle = "나의 독서 활동 통계 보기",
            icon = Icons.Default.BarChart,
            onClick = {
                isStatisticsMenuExpanded = !isStatisticsMenuExpanded
                isPrivacyMenuExpanded = false
                isActivityMenuExpanded = false
                isDisplayMenuExpanded = false
            }
        ),
        AccountMenuItem(
            type = MenuItemType.DISPLAY,
            title = "화면",
            subtitle = "다크 모드 등 화면 설정",
            icon = Icons.Default.Brightness4,
            onClick = {
                isDisplayMenuExpanded = !isDisplayMenuExpanded
                isPrivacyMenuExpanded = false
                isActivityMenuExpanded = false
                isStatisticsMenuExpanded = false
            }
        ),
        AccountMenuItem(
            type = MenuItemType.TERMS,
            title = "이용약관",
            subtitle = "서비스 이용 약관 확인",
            icon = Icons.Default.Description,
            onClick = {
                val url = "https://route-page.vercel.app/6"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        ),
        AccountMenuItem(
            type = MenuItemType.CONTACT,
            title = "문의하기",
            subtitle = "Readers 팀에 의견 남기기",
            icon = Icons.Default.HelpOutline,
            onClick = {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("readers.team.contact@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "[Readers 문의]")
                }
                try {
                    context.startActivity(emailIntent)
                } catch (e: Exception) {
                    // No email app found
                }
            }
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 계정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message)
                }
            }

            is ProfileUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(menuItems) { item ->
                        Column {
                            AccountMenuItemCard(item = item)

                            if (item.type == MenuItemType.PRIVACY) {
                                AnimatedVisibility(
                                    visible = isPrivacyMenuExpanded,
                                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(
                                        animationSpec = tween(300)
                                    ),
                                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    PrivacyToggle(
                                        isPrivate = state.user.isPrivate ?: false,
                                        onToggle = { newState ->
                                            viewModel.updateUserPrivacySetting(newState)
                                        },
                                        enabled = true
                                    )
                                }
                            }

                            if (item.type == MenuItemType.ACTIVITY) {
                                AnimatedVisibility(
                                    visible = isActivityMenuExpanded,
                                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(
                                        animationSpec = tween(300)
                                    ),
                                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    PostsSection(
                                        myPosts = state.myPosts,
                                        savedPosts = state.savedPosts,
                                        isMyProfile = state.isMyProfile,
                                        likedFeedIds = state.likedFeedIds,
                                        bookmarkedFeedIds = state.bookmarkedFeedIds,
                                        onLikeClick = viewModel::toggleLike,
                                        onBookmarkClick = viewModel::toggleBookmark,
                                        onDeleteClick = viewModel::deleteFeed,
                                        wishlist = state.wishlist,
                                        myLibrary = state.myLibrary,
                                        onToggleWishlist = viewModel::toggleWishlist,
                                        onToggleMyLibrary = viewModel::toggleMyLibrary,
                                        userInfoMap = state.userInfoMap
                                    )
                                }
                            }

                            if (item.type == MenuItemType.STATISTICS) {
                                AnimatedVisibility(
                                    visible = isStatisticsMenuExpanded,
                                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(
                                        animationSpec = tween(300)
                                    ),
                                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    StatisticsButton(onClick = onNavigateToStatistics)
                                }
                            }

                            if (item.type == MenuItemType.DISPLAY) {
                                AnimatedVisibility(
                                    visible = isDisplayMenuExpanded,
                                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(
                                        animationSpec = tween(300)
                                    ),
                                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    DarkModeToggle(
                                        isDarkMode = isDarkMode ?: false,
                                        onToggle = onDarkModeChange
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountMenuItemCard(item: AccountMenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = item.onClick)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkRed.copy(alpha = 0.1f))
                .padding(8.dp),
            tint = DarkRed
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun StatisticsButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "통계보기",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "통계 페이지로 이동",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
@Composable
fun PrivacyToggle(
    isPrivate: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .padding(top = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "비공개 계정",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Switch(
                checked = isPrivate,
                onCheckedChange = onToggle,
                enabled = enabled,
                thumbContent = {
                    Icon(
                        imageVector = if (isPrivate) Icons.Default.Check else Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                        tint = if (enabled) {
                            if (isPrivate) DarkRed else MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = DarkRed.copy(alpha = 0.5f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent,
                    disabledCheckedTrackColor = DarkRed.copy(alpha = 0.2f),
                    disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun DarkModeToggle(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(top = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "다크 모드",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Switch(
                checked = isDarkMode,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = DarkRed.copy(alpha = 0.5f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

// 이 아래에 PostsSection Composable을 정의하거나 import해야 합니다.
// 예시:
// @Composable
// fun PostsSection(...) { ... }
