package com.route.readers

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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.ui.screens.MainScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.onboarding.OnboardingScreen
import com.route.readers.ui.screens.profilesetup.ProfileSetupScreen
import com.route.readers.ui.screens.signup.SignUpScreen
import com.route.readers.ui.theme.ReadersTheme

val LocalAppNavController = staticCompositionLocalOf<NavHostController?> { null }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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
                            Toast.makeText(context, "사용자 정보 확인에 실패했습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
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
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        composable(
            route = "login_route/{from}",
            arguments = listOf(navArgument("from") { type = NavType.StringType })
        ) { backStackEntry ->
            val fromScreen = backStackEntry.arguments?.getString("from")

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
                    if (fromScreen == "signup") {
                        appNavController.popBackStack()
                    } else {
                        appNavController.navigate("onboarding_route") {
                            popUpTo(appNavController.graph.id) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("profile_setup_route") {
            ProfileSetupScreen(
                onSetupComplete = {
                    appNavController.navigate("main_app_content_route") {
                        popUpTo(appNavController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable("main_app_content_route") {
            MainScreen(
                onNavigateToLogin = {
                    appNavController.navigate("login_route/logout") {
                        popUpTo(appNavController.graph.id) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
