package com.route.readers.ui.screens.mylibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.Book
import com.route.readers.data.remote.MyLibraryRepository

@Composable
fun MyLibraryScreen(
    libraryRepository: MyLibraryRepository = MyLibraryRepository()
) {
    val myBooks by libraryRepository.myBooks.collectAsState()

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
                    text = "${myBooks.size}/10 권",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.Add, contentDescription = "책 추가")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Book Cards
        if (myBooks.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(myBooks) { book ->
                    BookCard(
                        book = book,
                        onUpdateProgress = { isbn, currentPage ->
                            libraryRepository.updateReadingProgress(isbn, currentPage)
                        }
                    )
                }
            }
        } else {
            // 빈 서재 상태
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📚",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "아직 서재에 책이 없습니다",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Text(
                        "검색에서 책을 추가해보세요!",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
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
fun BookCard(
    book: Book,
    onUpdateProgress: (String, Int) -> Unit
) {
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
            AsyncImage(
                model = book.cover.ifEmpty { null },
                contentDescription = "책 표지",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.readerslogo),
                placeholder = painterResource(R.mipmap.readerslogo)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Book Info
            Text(
                text = book.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
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
