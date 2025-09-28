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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.login.LoginScreen
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
    val startDestination = "login_route"

    NavHost(navController = appNavController, startDestination = startDestination) {
        composable("login_route") {
            LoginScreen(
                onLoginSuccess = {
                    appNavController.navigate("main_app_content_route") {
                        popUpTo("login_route") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    appNavController.navigate("signup_route")
                }
            )
        }

        composable("signup_route") {
            SignUpScreen(
                onSignUpSuccess = {
                    appNavController.navigate("main_app_content_route") {
                        popUpTo(appNavController.graph.startDestinationId) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    appNavController.popBackStack()
                }
            )
        }

        composable("main_app_content_route") {
            FeedScreen()
        }
    }
}
