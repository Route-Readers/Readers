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
import com.route.readers.ui.theme.TextGray
import com.route.readers.ui.theme.White

enum class MenuItemType {
    PRIVACY,
    ACTIVITY,
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
    viewModel: AccountViewModel = viewModel()
) {
    val context = LocalContext.current
    var isPrivacyMenuExpanded by remember { mutableStateOf(false) }
    var isActivityMenuExpanded by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    val menuItems = listOf(
        AccountMenuItem(
            type = MenuItemType.PRIVACY,
            title = "공개 범위",
            subtitle = "프로필 및 활동 공개 설정",
            icon = Icons.Default.Lock,
            onClick = { isPrivacyMenuExpanded = !isPrivacyMenuExpanded }
        ),
        AccountMenuItem(
            type = MenuItemType.ACTIVITY,
            title = "내 활동",
            subtitle = "내가 남긴 기록 확인하기",
            icon = Icons.Default.History,
            onClick = { isActivityMenuExpanded = !isActivityMenuExpanded }
        ),
        AccountMenuItem(
            type = MenuItemType.TERMS,
            title = "이용약관",
            subtitle = "서비스 이용 약관 확인",
            icon = Icons.Default.Description,
            onClick = {
                // val url = "https://your.terms.url"
                // val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                // context.startActivity(intent)
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
                    // 이메일 앱이 없는 경우 처리
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
                    containerColor = White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
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
                                        isPrivate = state.user.isPrivate,
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
            .background(White)
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
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                fontSize = 14.sp,
                color = TextGray
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
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
            .background(White)
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
                color = if (enabled) Color.Black else Color.Gray
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
                            if (isPrivate) DarkRed else Color.Gray
                        } else {
                            Color.LightGray
                        }
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = DarkRed.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color.LightGray,
                    checkedThumbColor = White,
                    uncheckedThumbColor = White,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent,
                    disabledCheckedTrackColor = DarkRed.copy(alpha = 0.2f),
                    disabledUncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )
        }
    }
}
