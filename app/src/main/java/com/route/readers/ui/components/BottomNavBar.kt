package com.route.readers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.route.readers.ui.theme.DarkRed
import com.route.readers.ui.theme.TextGray
import com.route.readers.ui.theme.White

@Composable
fun BottomNavBar(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(White),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem("피드", 0, selectedTab, onTabSelected)
        BottomNavItem("내서재", 1, selectedTab, onTabSelected)
        BottomNavItem("검색", 2, selectedTab, onTabSelected)
        BottomNavItem("커뮤니티", 3, selectedTab, onTabSelected)
        BottomNavItem("프로필", 4, selectedTab, onTabSelected)
    }
}

@Composable
fun BottomNavItem(
    text: String,
    index: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Text(
        text = text,
        color = if (selectedTab == index) DarkRed else TextGray,
        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clickable { onTabSelected(index) }
    )
}
