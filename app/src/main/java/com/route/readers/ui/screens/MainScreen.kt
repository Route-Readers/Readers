package com.route.readers.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
import com.route.readers.ui.screens.reading.ReadingTimerScreen
import com.route.readers.ui.screens.reading.ReadingViewModel
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

    onNavigateToUsedBookDetail: (String) -> Unit,
    onNavigateToChatList: () -> Unit
) {
    val context = LocalContext.current
    val bottomNavController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val readingViewModel: ReadingViewModel = viewModel()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    var selectedBook by remember { mutableStateOf<MyBook?>(null) }
    var timerCompletedBook by remember { mutableStateOf<MyBook?>(null) }
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showLogoutDialog by remember { mutableStateOf(false) }
    var bookToUpdateAfterReading by remember { mutableStateOf<MyBook?>(null) }
    var lastReadingSessionDuration by remember { mutableStateOf<Int?>(null) }
    var showFinishReadingDialogBook by remember { mutableStateOf<MyBook?>(null) }

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
                    onTokenClick = { 
                        Toast.makeText(context, "아직 공개되지 않은 기능이에요", Toast.LENGTH_SHORT).show()
                    }
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
                    onStartReading = {
                        selectedBook?.let { book ->
                            bottomNavController.navigate("reading_timer/${book.isbn}?startNow=true")
                        }
                    },
                    onPauseReading = { },
                    isTimerRunning = false
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
                    mainViewModel = mainViewModel,
                    onNavigateToSearch = {
                        bottomNavController.navigate(BottomNavItem.Search.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBookSelected = { book ->
                        selectedBook = book
                    },
                    bookToUpdate = bookToUpdateAfterReading,
                    onUpdateFinished = {
                        bookToUpdateAfterReading = null
                        lastReadingSessionDuration = null
                    },
                    lastReadingSessionDuration = lastReadingSessionDuration,
                    showFinishReadingDialogBook = showFinishReadingDialogBook,
                    onDismissFinishReadingDialog = {
                        showFinishReadingDialogBook = null
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
                    onNavigateToNotifications = { bottomNavController.navigate("notifications") },
                    onNavigateToUsedBookDetail = onNavigateToUsedBookDetail,
                    onNavigateToChatList = onNavigateToChatList,
                    isActive = currentRoute == BottomNavItem.Community.route
                )
            }
            composable("friends_list") {
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
                        },
                        onNavigateToGoal = {
                            navController.navigate("goal_route")
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
                        composable(
                            route = "reading_timer/{bookIsbn}?startNow={startNow}",
                            arguments = listOf(
                                navArgument("bookIsbn") { type = NavType.StringType },
                                navArgument("startNow") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            val bookIsbn = backStackEntry.arguments?.getString("bookIsbn")
                            val startNow = backStackEntry.arguments?.getBoolean("startNow") ?: false
                            selectedBook?.let { book ->
                                if (book.isbn == bookIsbn) {
                                    ReadingTimerScreen(
                                        book = book,
                                        onNavigateBack = { bottomNavController.popBackStack() },
                                        onFinishReading = { timeInSeconds ->
                                            showFinishReadingDialogBook = book
                                            bottomNavController.navigate(BottomNavItem.MyLibrary.route) {
                                                popUpTo(bottomNavController.graph.findStartDestination().id)
                                                launchSingleTop = true
                                            }
                                        },
                                        onDisposeReading = { timeInSeconds ->
                                            readingViewModel.saveReadingSession(book, timeInSeconds)
                                        },
                                        startNow = startNow
                                    )
                                }
                            }
                        }        }
    }
}
