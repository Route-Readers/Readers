package com.route.readers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.route.readers.notification.ReadingNotificationService
import com.route.readers.ui.screens.MainScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.login.LoginViewModel
import com.route.readers.ui.screens.login.OnboardingScreen
import com.route.readers.ui.screens.login.SignUpScreen
import com.route.readers.ui.screens.profile.FollowListScreen
import com.route.readers.ui.screens.profile.ProfileSetupScreen
import com.route.readers.ui.theme.ReadersTheme
import com.route.readers.widget.WidgetUpdateHelper
import java.net.URLDecoder
import java.net.URLEncoder

val LocalAppNavController = staticCompositionLocalOf<NavHostController?> { null }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WidgetUpdateHelper.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val notificationServiceIntent = Intent(this, ReadingNotificationService::class.java)
                startService(notificationServiceIntent)
            } else {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        } else {
            val notificationServiceIntent = Intent(this, ReadingNotificationService::class.java)
            startService(notificationServiceIntent)
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
                            val destination = if (document.exists() && document.getString("nickname") != null) {
                                "main_app_content_route"
                            } else {
                                "profile_setup_route"
                            }
                            appNavController.navigate(destination) {
                                popUpTo("decision_route") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "사용자 정보 확인에 실패했습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT)
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
                onNavigateToOtherUserProfile = { userId ->
                    appNavController.navigate("follow_list_route/$userId/followers/${""}")
                }
            )
        }

        composable(
            route = "follow_list_route/{userId}/{initialListType}/{nickname}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("initialListType") { type = NavType.StringType },
                navArgument("nickname") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val initialListType = backStackEntry.arguments?.getString("initialListType")
            val nickname = backStackEntry.arguments?.getString("nickname")?.let {
                URLDecoder.decode(it, "UTF-8")
            }
            if (userId != null && initialListType != null) {
                FollowListScreen(
                    userId = userId,
                    initialListType = initialListType,
                    nickname = nickname ?: "",
                    onUserClick = { clickedUserId ->
                        appNavController.navigate("profile_route/$clickedUserId")
                    },
                    onNavigateBack = { appNavController.popBackStack() }
                )
            }
        }
    }
}
