package com.route.readers.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Feed : BottomNavItem("feed_screen", "피드", Icons.Filled.Home)
    object MyLibrary : BottomNavItem("mylibrary_screen", "내 서재", Icons.Filled.Book)
    object Search : BottomNavItem("search_screen", "검색", Icons.Filled.Search)
    object Community : BottomNavItem("community_screen", "커뮤니티", Icons.Filled.Explore)
    object Profile : BottomNavItem("profile_screen", "프로필", Icons.Filled.AccountCircle)
}

val bottomNavItems = listOf(
    BottomNavItem.Feed,
    BottomNavItem.MyLibrary,
    BottomNavItem.Search,
    BottomNavItem.Community,
    BottomNavItem.Profile
)

@Composable
fun BottomNavBar(
    navController: NavController,
    onTabSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(modifier = modifier) {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        onTabSelected(screen)
                    }
                }
            )
        }
    }
}
