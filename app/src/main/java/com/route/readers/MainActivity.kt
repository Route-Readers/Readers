package com.route.readers

import UsedBookDetailScreen
import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.initialize
import com.route.readers.data.UserPreferencesRepository
import com.route.readers.notification.DailyNotificationScheduler
import com.route.readers.ui.screens.bookclub.BookClubChatScreen// BookClubChatScreen import 추가
import com.route.readers.ui.community.used_trade.ChatListScreen
import com.route.readers.ui.community.used_trade.ChatScreen
import com.route.readers.ui.screens.MainScreen
import com.route.readers.ui.screens.add_feed.AddFeedScreen
import com.route.readers.ui.screens.attendance.AttendanceScreen
import com.route.readers.ui.screens.attendance.AttendanceViewModel
import com.route.readers.ui.screens.challenge.ChallengeScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.login.LoginViewModel
import com.route.readers.ui.screens.login.OnboardingScreen
import com.route.readers.ui.screens.login.SignUpScreen
import com.route.readers.ui.screens.profile.AccountScreen
import com.route.readers.ui.screens.profile.AccountViewModel
import com.route.readers.ui.screens.profile.FollowListScreen
import com.route.readers.ui.screens.profile.GoalScreen
import com.route.readers.ui.screens.profile.LevelScreen
import com.route.readers.ui.screens.profile.MyBookListScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.profile.ProfileSetupScreen
import com.route.readers.ui.screens.profile.ProfileViewModel
import com.route.readers.ui.screens.profile.StatisticsScreen
import com.route.readers.ui.screens.token.TokenShopScreen
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

    private val accountViewModel: AccountViewModel by viewModels {
        AccountViewModelFactory(application, UserPreferencesRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        Firebase.initialize(context = this)
        val firebaseAppCheck = Firebase.appCheck
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
        
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

        // Extract notification data from the initial intent
        val initialNotificationType = intent.getStringExtra("notificationType")
        val initialBookClubId = intent.getStringExtra("bookClubId")
        val initialBookClubName = intent.getStringExtra("bookClubName")
        val initialChatId = intent.getStringExtra("chatId")
        val initialSenderId = intent.getStringExtra("senderId")
        val initialBookId = intent.getStringExtra("bookId")


        setupNotificationListener()
        setContent {
            val isSystemInDark = isSystemInDarkTheme()
            val isDarkMode by accountViewModel.isDarkMode.collectAsState()

            LaunchedEffect(isSystemInDark) {
                accountViewModel.syncWithSystemTheme(isSystemInDark)
            }

            ReadersTheme(darkTheme = isDarkMode ?: isSystemInDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val appNavController = rememberNavController()
                    CompositionLocalProvider(LocalAppNavController provides appNavController) {
                        RootAppNavigation(
                            accountViewModel = accountViewModel,
                            initialNotificationType = initialNotificationType,
                            initialBookClubId = initialBookClubId,
                            initialBookClubName = initialBookClubName,
                            initialChatId = initialChatId,
                            initialSenderId = initialSenderId,
                            initialBookId = initialBookId
                        )
                    }
                }
            }
        }
    }

    // --- 수정된 부분 1: onNewIntent 시그니처 변경 및 로직 수정 ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 새로운 intent로 Activity를 다시 생성하여 onCreate에서 처리하도록 함
        // 이 방식이 Compose 생명주기와 충돌하지 않고 안정적으로 상태를 갱신할 수 있음
        recreate()
    }

    // --- 수정된 부분 2: handleNotificationNavigation 함수 삭제 ---
    // 이 함수는 RootAppNavigation 내부의 LaunchedEffect로 로직이 이동했으므로 삭제합니다.

    private fun setupNotificationListener() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserId)
            .collection("notifications")
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val data = change.document.data
                        val title = data["title"] as? String ?: "알림"
                        val message = data["message"] as? String ?: ""

                        showLocalNotification(title, message)
                        change.document.reference.update("read", true)
                    }
                }
            }
    }

    private fun showLocalNotification(title: String, message: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reading_notifications",
                "독서 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat.Builder(this, "reading_notifications")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class AccountViewModelFactory(
    private val application: Application,
    private val repository: UserPreferencesRepository
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun RootAppNavigation(
    accountViewModel: AccountViewModel,
    initialNotificationType: String?,
    initialBookClubId: String?,
    initialBookClubName: String?,
    initialChatId: String? = null,
    initialSenderId: String? = null,
    initialBookId: String? = null
) {
    val appNavController = LocalAppNavController.current
        ?: throw IllegalStateException("LocalAppNavController not provided")

    val startDestination = "decision_route"

    // --- 수정된 부분 3: LaunchedEffect에서 내비게이션 로직 직접 처리 ---
    LaunchedEffect(initialNotificationType, initialBookClubId, initialBookClubName, initialChatId, initialSenderId, initialBookId) {
        if (initialNotificationType != null) {
            when (initialNotificationType) {
                "BOOK_CLUB_CHAT_MESSAGE" -> {
                    if (initialBookClubId != null && initialBookClubName != null) {
                        val encodedClubName = URLEncoder.encode(initialBookClubName, "UTF-8")
                        // 올바른 라우트로 이동하도록 수정
                        appNavController.navigate("bookclub_chat_route/$initialBookClubId/$encodedClubName") {
                            popUpTo(appNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
                "CHAT_MESSAGE" -> {
                    if (initialBookId != null && initialSenderId != null) {
                        appNavController.navigate("chat_route/$initialBookId/$initialSenderId") {
                            popUpTo(appNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
                // 다른 알림 타입에 대한 처리도 여기에 추가
            }
        }
    }

    NavHost(navController = appNavController, startDestination = startDestination) {

        composable("decision_route") {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val context = LocalContext.current

            // decision_route는 로그인/프로필 상태 확인 후 적절한 화면으로 보내주는 역할만 함
            // 알림 처리는 LaunchedEffect가 담당하므로 여기서 별도 처리는 불필요
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
                            // 알림으로 인한 초기 탐색이 아닐 경우에만 기본 로직 실행
                            if (initialNotificationType == null) {
                                appNavController.navigate(destination) {
                                    popUpTo("decision_route") { inclusive = true }
                                }
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
            // 알림으로 인해 decision_route에 머무는 동안 로딩 인디케이터 표시
            if(initialNotificationType == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
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
            val attendanceViewModel: AttendanceViewModel = viewModel()
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
                },
                onNavigateToChallenge = {
                    appNavController.navigate("challenge_route")
                },
                onNavigateToUsedBookDetail = { bookId ->
                    appNavController.navigate("used_book_detail_route/$bookId")
                },
                onNavigateToChatList = {
                    appNavController.navigate("chat_list_route")
                }
            )
        }

        composable("chat_list_route") {
            ChatListScreen(
                onNavigateBack = { appNavController.popBackStack() },
                onNavigateToChat = { otherUserId, bookId ->
                    appNavController.navigate("chat_route/$bookId/$otherUserId")
                }
            )
        }

        composable("account_route") {
            AccountScreen(
                onNavigateBack = { appNavController.popBackStack() },
                onNavigateToStatistics = { appNavController.navigate("statistics_route") },
                viewModel = accountViewModel,
                navController = appNavController // Pass navController
            )
        }

        composable("statistics_route") {
            StatisticsScreen(
                onNavigateBack = { appNavController.popBackStack() }
            )
        }

        composable("attendance_route") {
            val backStackEntry = remember(appNavController.currentBackStackEntry) {
                appNavController.getBackStackEntry("main_app_content_route")
            }
            val attendanceViewModel: AttendanceViewModel = viewModel(backStackEntry)
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
                val profileViewModel: ProfileViewModel =
                    viewModel()
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
                        appNavController.navigate("token_shop_route")
                    },
                    onNavigateToGoal = {
                        appNavController.navigate("goal_route")
                    },
                    onNavigateToOtherUserProfile = { otherUserId ->
                        appNavController.navigate("profile_route/$otherUserId")
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

        composable("goal_route") {
            GoalScreen(
                onBack = { appNavController.popBackStack() }
            )
        }

        composable("challenge_route") {
            ChallengeScreen()
        }

        composable("token_shop_route") {
            val context = LocalContext.current
            val rewardedAdManager = remember { com.route.readers.utils.RewardedAdManager(context) }
            TokenShopScreen(
                onNavigateBack = { appNavController.popBackStack() },
                rewardedAdManager = rewardedAdManager,
                onTokenEarned = { }
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
                    onNavigateBack = { appNavController.popBackStack() },
                    onNavigateToChat = { sellerId: String, bookIdForChat: String ->
                        appNavController.navigate("chat_route/$bookIdForChat/$sellerId")
                    }
                )

            }
        }

        composable(
            route = "chat_route/{bookId}/{otherUserId}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            val otherUserId = backStackEntry.arguments?.getString("otherUserId")
            if (bookId != null && otherUserId != null) {
                ChatScreen(
                    bookId = bookId,
                    sellerId = otherUserId,
                    onNavigateBack = { appNavController.popBackStack() },
                    onNavigateToDetail = { detailBookId ->
                        appNavController.navigate("used_book_detail_route/$detailBookId")
                    }
                )
            }
        }

        // Add bookclub chat route
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
                // BookClubChatScreen composable function
                BookClubChatScreen(
                    bookClubId = clubId,
                    bookClubName = clubName,
                    onBackClick = { appNavController.popBackStack() }
                )
            }
        }
    }
}
