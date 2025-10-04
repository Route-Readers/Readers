package com.route.readers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.onboarding.OnboardingScreen
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

    val startDestination = "splash_or_onboarding_decision"

    NavHost(navController = appNavController, startDestination = startDestination) {

        composable("splash_or_onboarding_decision") {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val destination = if (currentUser != null && currentUser.isEmailVerified) {
                "login_route/{from}"
            } else {
                "onboarding_route"
            }

            appNavController.navigate(destination.replace("{from}", "splash")) {
                popUpTo("splash_or_onboarding_decision") { inclusive = true }
            }
        }

        composable("onboarding_route") {
            OnboardingScreen(
                onNavigateToSignUp = { appNavController.navigate("signup_route") },
                onNavigateToLogin = { appNavController.navigate("login_route/onboarding") }
            )
        }

        composable(
            route = "login_route/{from}",
            arguments = listOf(navArgument("from") { type = NavType.StringType })
        ) { backStackEntry ->
            val fromScreen = backStackEntry.arguments?.getString("from")

            LoginScreen(
                onLoginSuccess = {
                    appNavController.navigate("main_app_content_route") {
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
                            popUpTo("login_route/{from}") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("signup_route") {
            SignUpScreen(
                onSignUpSuccess = {
                    appNavController.navigate("main_app_content_route") {
                        popUpTo(appNavController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    appNavController.navigate("login_route/signup") {}
                },
                onNavigateBack = {
                    appNavController.popBackStack()
                }
            )
        }

        composable("main_app_content_route") {
            FeedScreen()
        }
    }
}
