package com.route.readers.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.route.readers.data.model.MyBook
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
    var selectedBook by remember { mutableStateOf<MyBook?>(null) }
    var showProgressDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "friends_list") {
                BottomNavBar(
                    navController = bottomNavController,
                    onProfileClick = onNavigateToProfile,
                    selectedBook = selectedBook,
                    onStartReading = {
                        selectedBook?.let { book ->
                            // 내 서재로 이동하고 다이얼로그 표시
                            bottomNavController.navigate(BottomNavItem.MyLibrary.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            showProgressDialog = true
                        }
                    }
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
                selectedBook = null // 다른 탭으로 이동 시 선택 해제
                FeedScreen()
            }
            composable(BottomNavItem.MyLibrary.route) {
                MyLibraryScreen(
                    onBookSelected = { book ->
                        selectedBook = book
                    },
                    showProgressDialog = showProgressDialog,
                    onProgressDialogDismiss = { showProgressDialog = false }
                )
            }
            composable(BottomNavItem.Search.route) {
                selectedBook = null // 다른 탭으로 이동 시 선택 해제
                SearchScreen()
            }
            composable(BottomNavItem.Community.route) {
                selectedBook = null // 다른 탭으로 이동 시 선택 해제
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
