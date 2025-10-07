package com.route.readers.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.route.readers.ui.screens.MainScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.login.LoginViewModel
import com.route.readers.ui.screens.login.OnboardingScreen
import com.route.readers.ui.screens.login.SignUpScreen
import com.route.readers.ui.screens.profile.FollowListScreen
import com.route.readers.ui.screens.profile.ProfileSetupScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavigation(navController: NavHostController) {
    val startDestination = "decision_route"

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
                            val destination = if (document.exists() && document.getString("nickname") != null) {
                                "main_app_content_route"
                            } else {
                                "profile_setup_route"
                            }
                            navController.navigate(destination) {
                                popUpTo("decision_route") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "사용자 정보 확인에 실패했습니다.", Toast.LENGTH_SHORT).show()
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
                onNavigateToOtherUserProfile = { userId ->
                    navController.navigate("profile_route/$userId")
                }
            )
        }

        composable(
            route = "follow_list_route/{userId}/{initialListType}/{nickname}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("initialListType") { type = NavType.StringType },
                navArgument("nickname") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val initialListType = backStackEntry.arguments?.getString("initialListType")
            val nickname = backStackEntry.arguments?.getString("nickname")?.let {
                URLDecoder.decode(it, "UTF-8")
            }
            if (userId != null && initialListType != null && nickname != null) {
                FollowListScreen(
                    userId = userId,
                    initialListType = initialListType,
                    nickname = nickname,
                    onUserClick = { clickedUserId ->
                        navController.navigate("profile_route/$clickedUserId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
