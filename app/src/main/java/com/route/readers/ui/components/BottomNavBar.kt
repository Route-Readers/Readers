package com.route.readers.ui.components

import com.route.readers.data.model.MyBook
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Feed : BottomNavItem("feed_screen", "피드", Icons.Filled.Home)
    object MyLibrary : BottomNavItem("mylibrary_screen", "내 서재", Icons.Filled.AccountCircle)
    object Search : BottomNavItem("search_screen", "검색", Icons.Filled.Search)
    object Community : BottomNavItem("community_screen", "커뮤니티", Icons.Filled.Explore)
    object Profile : BottomNavItem("profile_route", "프로필", Icons.Filled.AccountCircle)
}

private val TAB_HEIGHT = 54.dp
private val INDICATOR_WIDTH = 51.dp
private val INDICATOR_HEIGHT = 51.dp
private val CENTER_BUTTON_SIZE = 54.dp

@Composable
fun BottomNavBar(
    navController: NavController,
    onProfileClick: () -> Unit,
    selectedBook: MyBook? = null,
    onStartReading: () -> Unit = {},
    onPauseReading: () -> Unit = {},
    isTimerRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // 현재 선택된 탭 인덱스 (0: 피드, 1: 검색, 2: 시작, 3: 커뮤니티, 4: 프로필)
    val selectedIndex = when {
        currentRoute == BottomNavItem.Feed.route -> 0
        currentRoute == BottomNavItem.Search.route -> 1
        currentRoute == BottomNavItem.Community.route -> 3
        currentRoute == "profile_route/{userId}" -> 4
        else -> -1
    }



    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        // 슬라이딩 인디케이터 배경
        if (selectedIndex >= 0 && selectedIndex != 2) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(TAB_HEIGHT)) {
                val totalWidth = maxWidth
                val itemWidth = totalWidth / 5
                
                val targetOffsetX = when (selectedIndex) {
                    0 -> itemWidth * 0.5f - INDICATOR_WIDTH / 2  // 첫 번째 탭 중심
                    1 -> itemWidth * 1.5f - INDICATOR_WIDTH / 2  // 두 번째 탭 중심
                    3 -> itemWidth * 3.5f - INDICATOR_WIDTH / 2  // 네 번째 탭 중심 (중앙 버튼 건너뛰기)
                    4 -> itemWidth * 4.5f - INDICATOR_WIDTH / 2  // 다섯 번째 탭 중심
                    else -> 0.dp
                }
                
                val animatedOffsetX by animateFloatAsState(
                    targetValue = targetOffsetX.value,
                    animationSpec = tween(durationMillis = 300),
                    label = "indicator_offset"
                )
                
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX.dp, y = (TAB_HEIGHT - INDICATOR_HEIGHT) / 2)
                        .width(INDICATOR_WIDTH)
                        .height(INDICATOR_HEIGHT)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                )
            }
        }

        // 5개 탭 균등 배치
        Row(
            modifier = Modifier.fillMaxWidth().height(TAB_HEIGHT),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 피드
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavItem(
                    item = BottomNavItem.Feed,
                    isSelected = selectedIndex == 0,
                    onClick = {
                        navController.navigate(BottomNavItem.Feed.route) {
                            popUpTo(BottomNavItem.Feed.route) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 검색
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavItem(
                    item = BottomNavItem.Search,
                    isSelected = selectedIndex == 1,
                    onClick = {
                        navController.navigate(BottomNavItem.Search.route) {
                            popUpTo(BottomNavItem.Feed.route) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 중앙 시작 버튼
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val isActive = selectedBook != null
                Box(
                    modifier = Modifier
                        .size(CENTER_BUTTON_SIZE)
                        .clip(CircleShape)
                        .then(
                            if (isActive) {
                                Modifier.background(MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier
                                    .background(Color.Transparent)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            }
                        )
                        .clickable {
                            if (selectedBook != null) {
                                if (isTimerRunning) onPauseReading() else onStartReading()
                            } else {
                                navController.navigate(BottomNavItem.MyLibrary.route) {
                                    popUpTo(BottomNavItem.Feed.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive && isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isActive) {
                            if (isTimerRunning) "독서 일시정지" else "독서 시작"
                        } else {
                            "내 서재로 이동"
                        },
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 커뮤니티
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavItem(
                    item = BottomNavItem.Community,
                    isSelected = selectedIndex == 3,
                    onClick = {
                        navController.navigate(BottomNavItem.Community.route) {
                            popUpTo(BottomNavItem.Feed.route) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 프로필
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavItem(
                    item = BottomNavItem.Profile,
                    isSelected = selectedIndex == 4,
                    onClick = { onProfileClick() }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .width(INDICATOR_WIDTH)
            .height(INDICATOR_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) selectedColor else unselectedColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.title,
                fontSize = 10.sp,
                color = if (isSelected) selectedColor else unselectedColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
