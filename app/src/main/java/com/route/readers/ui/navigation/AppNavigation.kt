package com.route.readers.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.route.readers.ui.components.BottomNavItem
import com.route.readers.ui.screens.community.CommunityScreen
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.mylibrary.MyLibraryScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.search.SearchScreen
import com.route.readers.ui.screens.signup.SignUpScreen


@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "login_or_initial_screen") {
        composable("login_or_initial_screen") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(BottomNavItem.Feed.route) {
                        popUpTo("login_or_initial_screen") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup_screen")
                }
            )
        }
        composable("signup_screen") {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(BottomNavItem.Feed.route) {
                        popUpTo("login_or_initial_screen") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(BottomNavItem.Feed.route) { FeedScreen() }
        composable(BottomNavItem.MyLibrary.route) { MyLibraryScreen() }
        composable(BottomNavItem.Search.route) { SearchScreen() }
        composable(BottomNavItem.Community.route) { CommunityScreen() }
        composable(BottomNavItem.Profile.route) { ProfileScreen() }
    }
}
