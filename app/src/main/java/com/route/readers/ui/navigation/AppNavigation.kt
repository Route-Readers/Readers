package com.route.readers.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.ui.community.used_trade.ChatScreen
import com.route.readers.ui.community.used_trade.UsedBookDetailScreen
import com.route.readers.ui.screens.MainScreen
import com.route.readers.ui.screens.add_feed.AddFeedScreen
import com.route.readers.ui.screens.attendance.AttendanceScreen
import com.route.readers.ui.screens.attendance.AttendanceViewModel
import com.route.readers.ui.screens.bookclub.BookClubScreen
import com.route.readers.ui.screens.bookclub.BookClubChatScreen
import com.route.readers.ui.screens.challenge.ChallengeScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.login.LoginViewModel
import com.route.readers.ui.screens.login.OnboardingScreen
import com.route.readers.ui.screens.login.SignUpScreen
import com.route.readers.ui.screens.profile.AccountScreen
import com.route.readers.ui.screens.profile.FollowListScreen
import com.route.readers.ui.screens.profile.FollowRequestsScreen
import com.route.readers.ui.screens.profile.FollowListViewModel
import com.route.readers.ui.screens.profile.GoalScreen
import com.route.readers.ui.screens.profile.LevelScreen
import com.route.readers.ui.screens.profile.MyBookListScreen
import com.route.readers.ui.screens.profile.ProfileCustomizationScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.profile.ProfileSetupScreen
import com.route.readers.ui.screens.profile.ProfileUiState
import com.route.readers.ui.screens.profile.ProfileViewModel
import com.route.readers.ui.screens.profile.StatisticsScreen
import com.route.readers.ui.screens.token.TokenShopScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavigation(navController: NavHostController) {
    val startDestination = "decision_route"

    val attendanceViewModel: AttendanceViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {

        composable("decision_route") {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    navController.navigate("onboarding_route") {
                        popUpTo("decision_route") { inclusive = true }
                    }
                } else {
                    firestore.collection("users").document(currentUser.uid).get()
                        .addOnSuccessListener { document ->
                            val destination =
                                if (document.exists() && document.getString("nickname") != null) {
                                    "main_app_content_route"
                                } else {
                                    "profile_setup_route"
                                }
                            navController.navigate(destination) {
                                popUpTo("decision_route") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "사용자 정보 확인에 실패했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            auth.signOut()
                            navController.navigate("onboarding_route") {
                                popUpTo("decision_route") { inclusive = true }
                            }
                        }
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable("onboarding_route") {
            OnboardingScreen(
                onNavigateToSignUp = { navController.navigate("signup_route") },
                onNavigateToLogin = { navController.navigate("login_route/onboarding") }
            )
        }

        composable("signup_route") {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate("profile_setup_route") {
                        popUpTo("signup_route") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login_route/signup")
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate("main_app_content_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "login_route/{from}",
            arguments = listOf(navArgument("from") { type = NavType.StringType })
        ) { backStackEntry ->
            val fromScreen = backStackEntry.arguments?.getString("from")
            val loginViewModel: LoginViewModel = viewModel()

            LoginScreen(
                onNavigateToHome = {
                    navController.navigate("main_app_content_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToCreateProfile = {
                    navController.navigate("profile_setup_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup_route")
                },
                onNavigateBack = {
                    if (fromScreen == "signup" || fromScreen == "verification" || fromScreen == "signup_success") {
                        navController.popBackStack()
                    } else {
                        navController.navigate("onboarding_route") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
                loginViewModel = loginViewModel
            )
        }

        composable("profile_setup_route") {
            ProfileSetupScreen(
                onSetupComplete = {
                    navController.navigate("main_app_content_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("main_app_content_route") {
            MainScreen(
                navController = navController,
                attendanceViewModel = attendanceViewModel,
                onNavigateToOtherUserProfile = { userId ->
                    navController.navigate("profile_route/$userId")
                },
                onNavigateToAddFeed = {
                    navController.navigate("add_feed_route")
                },
                onNavigateToMyAccount = {
                    navController.navigate("account_route")
                },
                onNavigateToAttendance = {
                    navController.navigate("attendance_route")
                },
                onNavigateToChallenge = {
                    navController.navigate("challenge_route")
                },
                onNavigateToUsedBookDetail = { bookId ->
                    navController.navigate("used_book_detail_route/$bookId")
                },
                onNavigateToChatList = {
                    navController.navigate("chat_list_route")
                }
            )
        }

        composable("token_shop_route") {
            TokenShopScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("account_route") {
            AccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStatistics = {
                    navController.navigate("statistics_route")
                }
            )
        }

        composable("statistics_route") {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
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
                        navController.navigate("search_route")
                    },
                    onNavigateToMyBookList = {
                        navController.navigate("my_book_list_route")
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

        composable("follow_requests_route") {
            FollowRequestsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = "follow_list_route/{userId}/{listType}/{nickname}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("listType") { type = NavType.StringType },
                navArgument("nickname") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val listType = backStackEntry.arguments?.getString("listType")
            val nickname = backStackEntry.arguments?.getString("nickname")?.let {
                URLDecoder.decode(it, "UTF-8")
            }

            if (userId != null && listType != null && nickname != null) {
                val followListViewModel: FollowListViewModel = viewModel()
                FollowListScreen(
                    userId = userId,
                    initialListType = listType,
                    nickname = nickname,
                    onUserClick = { otherUserId ->
                        navController.navigate("profile_route/$otherUserId") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFollowRequests = {
                        navController.navigate("follow_requests_route")
                    },
                    viewModel = followListViewModel
                )
            }
        }

        composable("profile_customization_route") {
            val profileViewModel: ProfileViewModel = viewModel()
            val uiState by profileViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null) {
                    profileViewModel.fetchUserProfile(currentUserId)
                }
            }

            val currentState = uiState
            if (currentState is ProfileUiState.Success && currentState.isMyProfile) {
                ProfileCustomizationScreen(
                    currentCharacter = currentState.user.profileCharacter,
                    currentBackgroundColor = currentState.user.profileBackgroundColor,
                    nickname = currentState.user.nickname,
                    onSave = { character, backgroundColor ->
                        profileViewModel.updateProfileCharacter(character, backgroundColor)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("my_book_list_route") {
            MyBookListScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("level_route") {
            LevelScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("goal_route") {
            GoalScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("add_feed_route") {
            AddFeedScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("attendance_route") {
            AttendanceScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = attendanceViewModel
            )
        }

        composable("challenge_route") {
            ChallengeScreen(
                onNavigateToChat = { clubId, clubName ->
                    val encodedClubName = URLEncoder.encode(clubName, "UTF-8")
                    navController.navigate("bookclub_chat_route/$clubId/$encodedClubName")
                }
            )
        }

        composable(
            route = "used_book_detail_route/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            if (bookId != null) {
                UsedBookDetailScreen(
                    bookId = bookId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { sellerId, bookIdForChat ->
                        navController.navigate("chat_route/$sellerId/$bookIdForChat")
                    }
                )
            }
        }

        composable(
            route = "chat_route/{sellerId}/{bookId}",
            arguments = listOf(
                navArgument("sellerId") { type = NavType.StringType },
                navArgument("bookId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId")
            val bookId = backStackEntry.arguments?.getString("bookId")
            if (sellerId != null && bookId != null) {
                ChatScreen(
                    sellerId = sellerId,
                    bookId = bookId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = "bookclub_chat_route/{clubId}/{clubName}",
            arguments = listOf(
                navArgument("clubId") { type = NavType.StringType },
                navArgument("clubName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val clubId = backStackEntry.arguments?.getString("clubId")
            val clubName = backStackEntry.arguments?.getString("clubName")?.let {
                URLDecoder.decode(it, "UTF-8")
            }
            if (clubId != null && clubName != null) {
                BookClubChatScreen(
                    bookClubId = clubId,
                    bookClubName = clubName,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
