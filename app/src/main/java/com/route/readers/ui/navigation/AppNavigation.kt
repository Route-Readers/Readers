package com.route.readers.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.onboarding.OnboardingScreen
import com.route.readers.ui.screens.signup.SignUpScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser == null) "onboarding_route" else "main_app_content_route"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("onboarding_route") {
            OnboardingScreen(
                onNavigateToSignUp = { navController.navigate("signup_route") },
                onNavigateToLogin = {
                    navController.navigate("login_route/onboarding")
                }
            )
        }

        composable(
            route = "login_route/{from}",
            arguments = listOf(navArgument("from") { type = NavType.StringType })
        ) { backStackEntry ->
            val fromScreen = backStackEntry.arguments?.getString("from")

            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main_app_content_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup_route")
                },
                onNavigateBack = {
                    if (fromScreen == "signup") {
                        navController.popBackStack()
                    } else {
                        navController.navigate("onboarding_route") {
                            popUpTo("login_route/{from}") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("signup_route") {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate("main_app_content_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login_route/signup")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("main_app_content_route") {
            FeedScreen()
        }
    }
}
