package com.route.readers.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.route.readers.ui.screens.attendance.AttendanceViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    attendanceViewModel: AttendanceViewModel,
    onNavigateToOtherUserProfile: (String) -> Unit,
    onNavigateToAddFeed: () -> Unit,
    onNavigateToMyAccount: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToChallenge: () -> Unit,
    onNavigateToTokenShop: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    var selectedBook by remember { mutableStateOf<MyBook?>(null) }
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("onboarding_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }) { Text("로그아웃") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            val shouldShowTopBar = currentRoute in listOf(
                BottomNavItem.Feed.route,
                BottomNavItem.MyLibrary.route,
                BottomNavItem.Search.route,
                BottomNavItem.Community.route,
                "profile_route/{userId}"
            )
            if (shouldShowTopBar) {
                val consecutiveReadingDays by attendanceViewModel.consecutiveReadingDays.collectAsState()
                val tokens by mainViewModel.tokens.collectAsState()
                
                FeedTopAppBar(
                    consecutiveReadingDays = consecutiveReadingDays,
                    tokens = tokens,
                    onBlockListClick = { bottomNavController.navigate("blockList") },
                    onLogoutClick = { showLogoutDialog = true },
                    onMyAccountClick = onNavigateToMyAccount,
                    onAttendanceClick = onNavigateToAttendance,
                    onNavigateToChallenge = onNavigateToChallenge,
                    onTokenClick = onNavigateToTokenShop
                )
            }
        },
        bottomBar = {
            val routesWithBottomBar = listOf(
                BottomNavItem.Feed.route,
                BottomNavItem.MyLibrary.route,
                BottomNavItem.Search.route,
                BottomNavItem.Community.route,
                "profile_route/{userId}"
            )
            if (currentRoute in routesWithBottomBar) {
                BottomNavBar(
                    navController = bottomNavController,
                    onProfileClick = {
                        if (currentUserId != null) {
                            bottomNavController.navigate("profile_route/$currentUserId") {
                                popUpTo(BottomNavItem.Feed.route) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    selectedBook = selectedBook,
                    onStartReading = { }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Feed.route,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            composable(BottomNavItem.Feed.route) {
                selectedBook = null
                FeedScreen(
                    attendanceViewModel = attendanceViewModel,
                    onNavigateToAddFeed = onNavigateToAddFeed,
                    onNavigateToOtherUserProfile = { userId ->
                        bottomNavController.navigate("profile_route/$userId")
                    },
                    onFollowBack = { }
                )
            }
            composable(BottomNavItem.MyLibrary.route) {
                MyLibraryScreen(
                    attendanceViewModel = attendanceViewModel,
                    onNavigateToSearch = {
                        bottomNavController.navigate(BottomNavItem.Search.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomNavItem.Search.route) {
                selectedBook = null
                SearchScreen()
            }
            composable(BottomNavItem.Community.route) {
                selectedBook = null
                val communityViewModel: CommunityViewModel = viewModel()
                CommunityScreen(
                    viewModel = communityViewModel,
                    onNavigateToFriendsList = { bottomNavController.navigate("friends_list") },
                    onNavigateToNotifications = { bottomNavController.navigate("notifications") }
                )
            }
            composable("friends_list") {
                val communityViewModel: CommunityViewModel = viewModel()
                AllUsersScreen(
                    onNavigateBack = { bottomNavController.popBackStack() },
                    onUserClick = { userId -> bottomNavController.navigate("profile_route/$userId") }
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
                        onNavigateToMyBookList = {
                            navController.navigate("my_book_list_route")
                        },
                        onNavigateToSearch = {
                            bottomNavController.navigate(BottomNavItem.Search.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToLevel = {
                            navController.navigate("level_route")
                        },
                        onNavigateToCustomization = {
                            navController.navigate("profile_customization_route")
                        }
                    )
                }
            }
            composable("notifications") {
                NotificationScreen(onNavigateBack = { bottomNavController.popBackStack() })
            }
            composable("blockList") {
                BlockedUserScreen(onNavigateBack = { bottomNavController.popBackStack() })
            }
        }
    }
}
