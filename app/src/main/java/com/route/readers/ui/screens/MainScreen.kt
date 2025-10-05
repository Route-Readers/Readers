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
import com.route.readers.ui.components.BottomNavBar
import com.route.readers.ui.components.BottomNavItem
import com.route.readers.ui.screens.community.AllUsersScreen // 이 import 문을 추가하세요.
import com.route.readers.ui.screens.community.CommunityScreen
import com.route.readers.ui.screens.community.CommunityViewModel
// import com.route.readers.ui.screens.community.FriendsListScreen // 더 이상 필요 없으므로 제거해도 됩니다.
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
            // "friends_list" 경로일 때 하단 바를 숨깁니다.
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
                        // '친구 목록 보기'를 누르면 "friends_list" 경로로 이동합니다.
                        bottomNavController.navigate("friends_list")
                    },
                    viewModel = communityViewModel
                )
            }
            // "friends_list" 경로가 요청되면 AllUsersScreen을 보여줍니다.
            composable("friends_list") {
                AllUsersScreen(
                    onNavigateBack = {
                        bottomNavController.popBackStack()
                    },
                    onUserClick = { userId ->
                        // 사용자 클릭 시 프로필 화면으로 이동하는 콜백을 전달합니다.
                        onNavigateToOtherUserProfile(userId)
                    }
                )
            }
        }
    }
}
