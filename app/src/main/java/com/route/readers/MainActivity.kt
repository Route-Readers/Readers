package com.route.readers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.route.readers.ui.components.BottomNavBar
import com.route.readers.ui.components.BottomNavItem
import com.route.readers.ui.screens.community.CommunityScreen
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.login.LoginScreen
import com.route.readers.ui.screens.mylibrary.MyLibraryScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.search.SearchScreen
import com.route.readers.ui.screens.signup.SignUpScreen
import com.route.readers.ui.theme.ReadersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            ReadersTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val routesWithBottomBar = listOf(
                    BottomNavItem.Feed.route,
                    BottomNavItem.MyLibrary.route,
                    BottomNavItem.Search.route,
                    BottomNavItem.Community.route,
                    BottomNavItem.Profile.route
                )

                Scaffold(
                    bottomBar = {
                        if (currentRoute in routesWithBottomBar) {
                            BottomNavBar(
                                navController = navController,
                                onTabSelected = { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "login_screen") {

        composable("login_screen") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(BottomNavItem.Feed.route) {
                        popUpTo("login_screen") { inclusive = true }
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
                        popUpTo("login_screen") { inclusive = true }
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
