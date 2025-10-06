package com.route.readers.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.ui.components.BottomNavBar
import com.route.readers.ui.components.BottomNavItem
import com.route.readers.ui.screens.community.AllUsersScreen
import com.route.readers.ui.screens.community.CommunityScreen
import com.route.readers.ui.screens.community.CommunityViewModel
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.mylibrary.MyLibraryScreen
import com.route.readers.ui.screens.search.SearchScreen

@Composable
fun MainScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToOtherUserProfile: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    val communityViewModel: CommunityViewModel = viewModel()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "friends_list") {
                BottomNavBar(
                    navController = bottomNavController,
                    onProfileClick = onNavigateToProfile
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Feed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Feed.route) {
                FeedScreen()
            }
            composable(BottomNavItem.MyLibrary.route) {
                MyLibraryScreen()
            }
            composable(BottomNavItem.Search.route) {
                SearchScreen()
            }
            composable(BottomNavItem.Community.route) {
                CommunityScreen(
                    onNavigateToFriendsList = {
                        bottomNavController.navigate("friends_list")
                    },
                    viewModel = communityViewModel
                )
            }
            composable("friends_list") {
                AllUsersScreen(
                    onNavigateBack = {
                        bottomNavController.popBackStack()
                    },
                    onUserClick = onNavigateToOtherUserProfile
                )
            }
        }
    }
}
