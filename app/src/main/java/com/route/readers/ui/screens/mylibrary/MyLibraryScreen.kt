package com.route.readers.ui.screens.mylibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.data.model.Book

@Composable
fun MyLibraryScreen() {
    val books = listOf(
        Book(title = "아토믹 해빗", author = "제임스 클리어", currentPage = 47, totalPages = 320, progress = 15),
        Book(title = "사피엔스", author = "유발 하라리", currentPage = 123, totalPages = 435, progress = 28),
        Book(title = "데미안", author = "헤르만 헤세", currentPage = 78, totalPages = 240, progress = 33)
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "내 서재",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "3/10 권",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Row {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("브랜딩 보기", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Add, contentDescription = "책 추가")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Book Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(books) { book ->
                BookCard(book = book)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Start Reading Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = { },
                    containerColor = Color.Blue,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "독서 시작",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "독서 시작하기",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun BookCard(book: Book) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Book Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("책 표지", color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Book Info
            Text(
                text = book.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = book.author,
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${book.currentPage}페이지",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${book.progress}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue
                )
                Text(
                    text = "${book.totalPages}페이지",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = book.progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.Blue,
                trackColor = Color.LightGray
            )
        }
    }
}
