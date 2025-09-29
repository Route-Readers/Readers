package com.route.readers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Feed : BottomNavItem("feed_screen", "피드", Icons.Filled.Home)
    object MyLibrary : BottomNavItem("mylibrary_screen", "내 서재", Icons.Filled.AccountCircle)
    object Search : BottomNavItem("search_screen", "검색", Icons.Filled.Search)
    object Community : BottomNavItem("community_screen", "커뮤니티", Icons.Filled.Explore)
    object Profile : BottomNavItem("profile_screen", "프로필", Icons.Filled.AccountCircle)
}

val sideNavItems = listOf(
    BottomNavItem.Feed,
    BottomNavItem.Search,
    BottomNavItem.Community,
    BottomNavItem.Profile
)

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 16.dp)
    ) {
        // 중앙 플레이 버튼
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF4285F4))
                .clickable {
                    navController.navigate(BottomNavItem.MyLibrary.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "독서 시작",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // 좌측 네비게이션 아이템들 (2개)
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            sideNavItems.take(2).forEach { screen ->
                NavItem(
                    item = screen,
                    isSelected = currentRoute == screen.route,
                    onClick = {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }

        // 우측 네비게이션 아이템들 (2개)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            sideNavItems.drop(2).forEach { screen ->
                NavItem(
                    item = screen,
                    isSelected = currentRoute == screen.route,
                    onClick = {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isSelected) Color(0xFF4285F4) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF4285F4) else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
