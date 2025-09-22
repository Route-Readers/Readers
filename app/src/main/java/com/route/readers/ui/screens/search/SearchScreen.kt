package com.route.readers.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkRed,
            contentColor = White
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("도서검색") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("도서관검색") }
            )
        }
        
        when (selectedTab) {
            0 -> BookSearchTab()
            1 -> LibrarySearchTab()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchTab() {
    var searchText by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("책 제목, 작가명으로 검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "맞춤 추천",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "읽은 책 기반으로 추천해드려요",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(3) { index ->
                            BookRecommendationCard("추천 도서 ${index + 1}")
                        }
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "필터 검색",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("소설", "에세이", "자기계발").forEach { genre ->
                            FilterChip(
                                onClick = {},
                                label = { Text(genre) },
                                selected = false
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("2024년", "2023년", "평점 4.0+").forEach { filter ->
                            FilterChip(
                                onClick = {},
                                label = { Text(filter) },
                                selected = false
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "인기 검색어",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(5) { index ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}",
                                    color = DarkRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text("인기 도서 ${index + 1}", color = TextGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookRecommendationCard(title: String) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(160.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ReadingGreen, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📚", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LibrarySearchTab() {
    var searchText by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("지역명으로 검색 (예: 강남구)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🗺️",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "지도 영역",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkRed
                        )
                        Text(
                            "(지도 API 연동 예정)",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "주변 서점/도서관",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(3) { index ->
                            LibraryItem("서점/도서관 ${index + 1}", "${index + 1}km")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryItem(name: String, distance: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CreamBackground, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                "서울시 강남구",
                color = TextGray,
                fontSize = 12.sp
            )
        }
        Text(
            distance,
            color = DarkRed,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
