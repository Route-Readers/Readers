package com.route.readers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.route.readers.ui.screens.feed.FeedScreen
import com.route.readers.ui.screens.search.SearchScreen
import com.route.readers.ui.components.BottomNavBar
import com.route.readers.navigation.AppNavigation


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedTab by remember { mutableStateOf(0) }
            
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                    Column {
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> FeedScreen()
                                2 -> SearchScreen()
                                else -> FeedScreen() // 임시로 피드 화면 표시
                            }
                        }
                        BottomNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                }
            }
        }
    }
}
