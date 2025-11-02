package com.route.readers

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.notification.DailyNotificationScheduler
import com.route.readers.ui.screens.MainScreen
import com.route.readers.ui.screens.add_feed.AddFeedScreen
import com.route.readers.ui.screens.attendance.AttendanceScreen
import com.route.readers.ui.screens.attendance.AttendanceViewModel
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.login.LoginViewModel
import com.route.readers.ui.screens.login.OnboardingScreen
import com.route.readers.ui.screens.login.SignUpScreen
import com.route.readers.ui.screens.profile.AccountScreen
import com.route.readers.ui.screens.profile.FollowListScreen
import com.route.readers.ui.screens.profile.LevelScreen
import com.route.readers.ui.screens.profile.MyBookListScreen
import com.route.readers.ui.screens.profile.ProfileCustomizationScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.profile.ProfileSetupScreen
import com.route.readers.ui.screens.profile.ProfileUiState
import com.route.readers.ui.screens.profile.ProfileViewModel
import com.route.readers.ui.theme.ReadersTheme
import com.route.readers.widget.WidgetUpdateHelper
import java.net.URLDecoder
import java.net.URLEncoder

val LocalAppNavController = staticCompositionLocalOf<NavHostController?> { null }

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DailyNotificationScheduler.scheduleDailyNotification(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WidgetUpdateHelper.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                DailyNotificationScheduler.scheduleDailyNotification(this)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            DailyNotificationScheduler.scheduleDailyNotification(this)
        }
        setContent {
            val appNavController = rememberNavController()
            ReadersTheme {
                CompositionLocalProvider(LocalAppNavController provides appNavController) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        RootAppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun RootAppNavigation() {
    val appNavController = LocalAppNavController.current
        ?: throw IllegalStateException("LocalAppNavController not provided")

    val startDestination = "decision_route"
    val attendanceViewModel: AttendanceViewModel = viewModel()

    NavHost(navController = appNavController, startDestination = startDestination) {

        composable("decision_route") {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    appNavController.navigate("onboarding_route") {
                        popUpTo("decision_route") { inclusive = true }
                    }
                } else if (!currentUser.isEmailVerified) {
                    auth.signOut()
                    Toast.makeText(context, "이메일 인증을 완료해주세요.", Toast.LENGTH_LONG).show()
                    appNavController.navigate("login_route/verification") {
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
                            appNavController.navigate(destination) {
                                popUpTo("decision_route") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "사용자 정보 확인에 실패했습니다. 다시 로그인해주세요.",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            auth.signOut()
                            appNavController.navigate("onboarding_route") {
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
                onNavigateToSignUp = { appNavController.navigate("signup_route") },
                onNavigateToLogin = { appNavController.navigate("login_route/onboarding") }
            )
        }

        composable("signup_route") {
            SignUpScreen(
                onSignUpSuccess = {
                    appNavController.navigate("profile_setup_route") {
                        popUpTo("signup_route") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    appNavController.navigate("login_route/signup")
                },
                onNavigateBack = {
                    appNavController.popBackStack()
                },
                onNavigateToHome = {
                    appNavController.navigate("main_app_content_route") {
                        popUpTo(appNavController.graph.id) { inclusive = true }
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
                    appNavController.navigate("main_app_content_route") {
                        popUpTo(appNavController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToCreateProfile = {
                    appNavController.navigate("profile_setup_route") {
                        popUpTo(appNavController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    appNavController.navigate("signup_route")
                },
                onNavigateBack = {
                    if (fromScreen == "signup" || fromScreen == "verification" || fromScreen == "signup_success") {
                        appNavController.popBackStack()
                    } else {
                        appNavController.navigate("onboarding_route") {
                            popUpTo(appNavController.graph.id) { inclusive = true }
                        }
                    }
                },
                loginViewModel = loginViewModel
            )
        }

        composable("profile_setup_route") {
            ProfileSetupScreen(
                onSetupComplete = {
                    appNavController.navigate("main_app_content_route") {
                        popUpTo(appNavController.graph.id) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    appNavController.popBackStack()
                }
            )
        }

        composable("main_app_content_route") {
            MainScreen(
                navController = appNavController,
                attendanceViewModel = attendanceViewModel,
                onNavigateToOtherUserProfile = { userId ->
                    appNavController.navigate("profile_route/$userId")
                },
                onNavigateToAddFeed = {
                    appNavController.navigate("add_feed_route")
                },
                onNavigateToMyAccount = {
                    appNavController.navigate("account_route")
                },
                onNavigateToAttendance = {
                    appNavController.navigate("attendance_route")
                }
            )
        }

        composable("account_route") {
            AccountScreen(
                onNavigateBack = { appNavController.popBackStack() },
                onNavigateToPrivacy = {}
            )
        }

        composable("attendance_route") {
            AttendanceScreen(
                onNavigateBack = { appNavController.popBackStack() },
                viewModel = attendanceViewModel
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
                        appNavController.navigate("follow_list_route/$userId/$listType/$encodedNickname")
                    },
                    onNavigateToSearch = {
                    },
                    onNavigateToMyBookList = {
                        appNavController.navigate("my_book_list_route")
                    },
                    onNavigateToLevel = {
                        appNavController.navigate("level_route")
                    },
                    onNavigateToCustomization = {
                        appNavController.navigate("profile_customization_route")
                    }
                )
            }
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
                FollowListScreen(
                    userId = userId,
                    initialListType = listType,
                    nickname = nickname,
                    onUserClick = { otherUserId ->
                        appNavController.navigate("profile_route/$otherUserId") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = { appNavController.popBackStack() }
                )
            }
        }

        composable("add_feed_route") {
            AddFeedScreen(
                onNavigateBack = { appNavController.popBackStack() }
            )
        }

        composable("my_book_list_route") {
            MyBookListScreen(
                onBack = { appNavController.popBackStack() }
            )
        }

        composable("level_route") {
            LevelScreen(
                onBack = { appNavController.popBackStack() }
            )
        }

        composable("profile_customization_route") {
            val profileViewModel: ProfileViewModel = viewModel()
            val uiState = profileViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null) {
                    profileViewModel.fetchUserProfile(currentUserId)
                }
            }

            val currentState = uiState.value
            if (currentState is ProfileUiState.Success && currentState.isMyProfile) {
                ProfileCustomizationScreen(
                    currentCharacter = currentState.user.profileCharacter,
                    currentBackgroundColor = currentState.user.profileBackgroundColor,
                    nickname = currentState.user.nickname,
                    onSave = { character, backgroundColor ->
                        profileViewModel.updateProfileCharacter(character, backgroundColor)
                        appNavController.popBackStack()
                    },
                    onBack = { appNavController.popBackStack() }
                )
            }
        }
    }
}