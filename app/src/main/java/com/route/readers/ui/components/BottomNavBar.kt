package com.route.readers.ui.components

import com.route.readers.data.model.MyBook
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Feed : BottomNavItem("feed_screen", "피드", Icons.Filled.Home)
    object MyLibrary : BottomNavItem("mylibrary_screen", "내 서재", Icons.Filled.AccountCircle)
    object Search : BottomNavItem("search_screen", "검색", Icons.Filled.Search)
    object Community : BottomNavItem("community_screen", "커뮤니티", Icons.Filled.Explore)
    object Profile : BottomNavItem("profile_route", "프로필", Icons.Filled.AccountCircle)
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
    onProfileClick: () -> Unit,
    selectedBook: MyBook? = null,
    onStartReading: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(vertical = 8.dp)
    ) {
        val centerButtonColor =
            if (selectedBook != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(centerButtonColor)
                .clickable {
                    if (selectedBook != null) {
                        onStartReading()
                    } else {
                        navController.navigate(BottomNavItem.MyLibrary.route) {
                            popUpTo(BottomNavItem.Feed.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = if (selectedBook != null) "독서 시작" else "내 서재",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            sideNavItems.take(2).forEach { screen ->
                val isSelected = if (screen.route == "profile_route") {
                    val profileUserId = navBackStackEntry?.arguments?.getString("userId")
                    currentRoute == "profile_route/{userId}" && profileUserId == currentUserId
                } else {
                    currentRoute == screen.route
                }
                NavItem(
                    item = screen,
                    isSelected = isSelected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(BottomNavItem.Feed.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            sideNavItems.drop(2).forEach { screen ->
                val isSelected = if (screen.route == "profile_route") {
                    val profileUserId = navBackStackEntry?.arguments?.getString("userId")
                    currentRoute == "profile_route/{userId}" && profileUserId == currentUserId
                } else {
                    currentRoute == screen.route
                }
                NavItem(
                    item = screen,
                    isSelected = isSelected,
                    onClick = {
                        if (screen.route == BottomNavItem.Profile.route) {
                            onProfileClick()
                        } else {
                            navController.navigate(screen.route) {
                                popUpTo(BottomNavItem.Feed.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
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
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isSelected) selectedColor else unselectedColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            fontSize = 12.sp,
            color = if (isSelected) selectedColor else unselectedColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
