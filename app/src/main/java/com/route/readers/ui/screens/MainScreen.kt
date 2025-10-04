package com.route.readers.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.route.readers.LocalAppNavController
import com.route.readers.ui.components.BottomNavBar
import com.route.readers.ui.components.BottomNavItem
import com.route.readers.ui.screens.community.CommunityScreen
import com.route.readers.ui.screens.community.CommunityViewModel
import com.route.readers.ui.screens.community.FriendsListScreen
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.mylibrary.MyLibraryScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.search.SearchScreen

@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit
) {
    val appNavController = LocalAppNavController.current
        ?: throw IllegalStateException("LocalAppNavController not provided")

    val bottomNavController = rememberNavController()
    val communityViewModel: CommunityViewModel = viewModel()

    Scaffold(
        bottomBar = { BottomNavBar(navController = bottomNavController) }
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
                FriendsListScreen(
                    onBackClick = {
                        bottomNavController.popBackStack()
                    },
                    viewModel = communityViewModel
                )
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { bottomNavController.popBackStack() },
                    onNavigateToLogin = onNavigateToLogin,
                    onFollowersClick = { /* TODO: 팔로워 화면으로 이동 */ },
                    onFollowingClick = { /* TODO: 팔로잉 화면으로 이동 */ }
                )
            }
        }
    }
}
