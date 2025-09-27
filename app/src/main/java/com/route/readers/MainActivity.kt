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
import com.route.readers.ui.screens.mylibrary.MyLibraryScreen
import com.route.readers.ui.screens.search.SearchScreen
import com.route.readers.ui.screens.community.CommunityScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.components.BottomNavBar
import com.route.readers.ui.theme.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedTab by remember { mutableStateOf(0) }

            ReadersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> FeedScreen()
                                1 -> MyLibraryScreen()
                                2 -> SearchScreen()
                                3 -> CommunityScreen()
                                4 -> ProfileScreen()
                                else -> FeedScreen()
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
