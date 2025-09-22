package com.route.readers.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .background(White)
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("책 제목, 작가명으로 검색") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightGray)
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
                    Text("읽은 책 기반 추천 도서가 여기에 표시됩니다")
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightGray)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = {},
                            label = { Text("장르") },
                            selected = false
                        )
                        FilterChip(
                            onClick = {},
                            label = { Text("출간년도") },
                            selected = false
                        )
                        FilterChip(
                            onClick = {},
                            label = { Text("평점") },
                            selected = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibrarySearchTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("지역명으로 검색") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = LightGray)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "지도 영역\n(지도 API 연동 예정)",
                        color = TextGray
                    )
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightGray)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("위치 기반 서점/도서관 목록이 여기에 표시됩니다")
                }
            }
        }
    }
}
