package com.route.readers.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.MyBook
import com.route.readers.ui.components.BottomNavBar
import com.route.readers.ui.components.BottomNavItem
import com.route.readers.ui.screens.community.AllUsersScreen
import com.route.readers.ui.screens.community.CommunityScreen
import com.route.readers.ui.screens.community.CommunityViewModel
import com.route.readers.ui.screens.community.NotificationScreen
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.feed.FeedTopAppBar
import com.route.readers.ui.screens.mylibrary.MyLibraryScreen
import com.route.readers.ui.screens.profile.BlockedUserScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.profile.ProfileViewModel
import com.route.readers.ui.screens.search.SearchScreen
import java.net.URLEncoder

@Composable
fun MainScreen(
    navController: NavHostController,
    onNavigateToOtherUserProfile: (String) -> Unit,
    onNavigateToAddFeed: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val communityViewModel: CommunityViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel()

    var selectedBook by remember { mutableStateOf<MyBook?>(null) }
    var showProgressDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showLogoutDialog by remember { mutableStateOf(false) }

    val routesWithFeedTopBar = listOf(
        BottomNavItem.Feed.route,
        BottomNavItem.MyLibrary.route,
        BottomNavItem.Search.route,
        BottomNavItem.Community.route,
        "profile_route/{userId}"
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("onboarding_route") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                ) { Text("로그아웃") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (currentRoute in routesWithFeedTopBar) {
                val consecutiveDays by mainViewModel.consecutiveDays.collectAsState()
                FeedTopAppBar(
                    consecutiveDays = consecutiveDays,
                    onBlockListClick = {
                        bottomNavController.navigate("blockList")
                    },
                    onLogoutClick = {
                        showLogoutDialog = true
                    }
                )
            }
        },
        bottomBar = {
            val routesWithBottomBar = routesWithFeedTopBar
            if (currentRoute in routesWithBottomBar) {
                BottomNavBar(
                    navController = bottomNavController,
                    onProfileClick = {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        if (currentUserId != null) {
                            bottomNavController.navigate("profile_route/$currentUserId") {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    selectedBook = selectedBook,
                    onStartReading = {
                        selectedBook?.let {
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
                selectedBook = null
                FeedScreen(
                    onNavigateToAddFeed = onNavigateToAddFeed
                )
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
                selectedBook = null
                SearchScreen()
            }
            composable(BottomNavItem.Community.route) {
                selectedBook = null
                CommunityScreen(
                    onNavigateToFriendsList = {
                        bottomNavController.navigate("friends_list")
                    },
                    onNavigateToNotifications = {
                        bottomNavController.navigate("notifications")
                    },
                    viewModel = communityViewModel
                )
            }
            composable("friends_list") {
                AllUsersScreen(
                    onNavigateBack = {
                        bottomNavController.popBackStack()
                    },
                    onUserClick = { userId ->
                        bottomNavController.navigate("profile_route/$userId")
                    }
                )
            }
            composable(
                route = "profile_route/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                if (userId != null) {
                    val profileViewModel: ProfileViewModel = viewModel()
                    ProfileScreen(
                        userId = userId,
                        viewModel = profileViewModel,
                        onNavigateToFollowList = { listType, nickname ->
                            val encodedNickname = URLEncoder.encode(nickname, "UTF-8")
                            navController.navigate("follow_list_route/$userId/$listType/$encodedNickname")
                        },
                        onNavigateToSearch = {
                            bottomNavController.navigate(BottomNavItem.Search.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
            composable("notifications") {
                NotificationScreen(
                    onNavigateBack = {
                        bottomNavController.popBackStack()
                    }
                )
            }
            composable("blockList") {
                BlockedUserScreen(onNavigateBack = { bottomNavController.popBackStack() })
            }
        }
    }
}
