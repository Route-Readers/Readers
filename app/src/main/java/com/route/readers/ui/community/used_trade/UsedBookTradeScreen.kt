package com.route.readers.ui.community.used_trade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UsedBookTradeScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 100.dp
        )
    ) {
        item {
            // 토큰으로 중고책 거래 안내
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "토큰으로 중고책 거래",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "친구들이 읽은 책을 토큰으로 구매해보세요!",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 친구들의 중고책 섹션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "친구들의 중고책",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = { /* 내 책 등록 */ }) {
                    Text("내 책 등록", color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        items(3) { index ->
            val books = listOf(
                Triple("아토믹 해빗", "제임스 클리어", "김지연"),
                Triple("사피엔스", "유발 하라리", "박민수"),
                Triple("데미안", "헤르만 헤세", "최동욱")
            )
            val (title, author, seller) = books[index]
            val tokens = listOf(15, 12, 8)[index]
            
            UsedBookItem(
                title = title,
                author = author,
                seller = seller,
                tokens = tokens,
                condition = "상급",
                rating = 4.6f + (index * 0.1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
