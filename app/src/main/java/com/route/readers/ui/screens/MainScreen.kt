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
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.mylibrary.MyLibraryScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.profile.ProfileViewModel
import com.route.readers.ui.screens.search.SearchScreen
import java.net.URLEncoder

@Composable
fun MainScreen(
    navController: NavHostController,
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
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        currentUserId?.let {
                            bottomNavController.navigate("profile_route/$it") {
                                popUpTo(bottomNavController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    },
                    selectedBook = selectedBook,
                    onStartReading = {
                        selectedBook?.let { book ->
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
                selectedBook = null
                SearchScreen()
            }
            composable(BottomNavItem.Community.route) {
                selectedBook = null
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
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("onboarding_route") {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                        onNavigateToFollowList = { listType, nickname ->
                            val encodedNickname = URLEncoder.encode(nickname, "UTF-8")
                            navController.navigate("follow_list_route/$userId/$listType/$encodedNickname")
                        },
                        onNavigateBack = {
                            bottomNavController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
