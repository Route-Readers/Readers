package com.route.readers.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

        // 1. 온보딩 화면
        composable("onboarding_route") {
            OnboardingScreen(
                onNavigateToSignUp = { navController.navigate("signup_route") },
                onNavigateToLogin = { navController.navigate("login_route") }
            )
        }

        // 2. 로그인 화면
        composable("login_route") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main_app_content_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {

                    navController.navigate("signup_route")
                }
            )
        }

        // 3. 회원가입 화면
        composable("signup_route") {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate("main_app_content_route") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("main_app_content_route") {
            FeedScreen()
        }
    }
}
