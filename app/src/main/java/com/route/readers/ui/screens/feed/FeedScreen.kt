package com.route.readers.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.route.readers.ui.theme.CreamBackground
import com.route.readers.ui.theme.DarkRed
import com.route.readers.ui.theme.ReadingGreen
import com.route.readers.ui.theme.TextGray
import com.route.readers.ui.theme.White
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FeedScreen(feedViewModel: FeedViewModel = viewModel()) {
    val uiState by feedViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is FeedUiState.Loading -> CircularProgressIndicator()
            is FeedUiState.Error -> Text(text = state.message)
            is FeedUiState.Success -> {
                if (state.items.isEmpty()) {
                    Text("표시할 피드가 없습니다.")
                } else {
                    ActualFeedContent(feedItems = state.items)
                }
            }
        }
    }
}

@Composable
fun ActualFeedContent(feedItems: List<FeedItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(feedItems) { item ->
            FeedCard(item = item)
        }
    }
}

@Composable
fun FeedCard(item: FeedItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.userName.firstOrNull()?.toString() ?: "R",
                            color = White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = item.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkRed)
                }
                Text(text = formatTimestamp(item.timestamp), color = TextGray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (item) {
                is FeedItem.Follow -> {
                    Text(text = "${item.follower}님이 ${item.following}님을 팔로우합니다.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkRed)) {
                        Text("맞팔하기", color = White)
                    }
                }
                is FeedItem.ChallengeStart -> {
                    Text(text = item.description, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    // ▼▼▼ 오류 수정: 람다({}) 제거 ▼▼▼
                    LinearProgressIndicator(progress = 0.1f, modifier = Modifier.fillMaxWidth(), color = DarkRed)
                    Text("10% 완료", fontSize = 12.sp, color = TextGray)
                }
                is FeedItem.ChallengeSuccess -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎉", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = item.description, fontSize = 14.sp, color = DarkRed, fontWeight = FontWeight.Medium)
                    }
                }
                is FeedItem.ReadingProgress -> {
                    Text(text = "📖 ${item.bookTitle}", fontWeight = FontWeight.Medium, color = DarkRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.description, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (item.totalPages > 0) {
                        // ▼▼▼ 오류 수정: 람다({}) 제거 ▼▼▼
                        LinearProgressIndicator(
                            progress = item.currentPage.toFloat() / item.totalPages,
                            modifier = Modifier.fillMaxWidth(),
                            color = ReadingGreen
                        )
                        Text("${item.currentPage}/${item.totalPages}페이지", fontSize = 12.sp, color = TextGray)
                    }
                }
                is FeedItem.BookReview -> {
                    Text(text = "📚 ${item.bookTitle}", fontWeight = FontWeight.Medium, color = DarkRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = if (index < item.rating) DarkRed else Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${item.rating}/5", fontSize = 12.sp, color = TextGray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.review, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                var isLiked by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = { isLiked = !isLiked }) {
                        Icon(imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "좋아요", tint = if (isLiked) DarkRed else TextGray)
                    }
                    Text(text = item.likeCount.toString(), fontSize = 14.sp, color = TextGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    TextButton(onClick = {}) {
                        Text("💬 ${item.commentCount}", fontSize = 14.sp, color = TextGray)
                    }
                }
            }
        }
    }
}

sealed class FeedItem(
    open val userName: String = "",
    open val timestamp: Timestamp = Timestamp.now(),
    open val likeCount: Int = 0,
    open val commentCount: Int = 0,
    open val type: String = ""
) {
    data class Follow(
        val follower: String = "",
        val following: String = "",
        override val timestamp: Timestamp = Timestamp.now()
    ) : FeedItem(userName = follower, timestamp = timestamp, type = "FOLLOW")

    data class ChallengeStart(
        val users: String = "",
        val description: String = "",
        override val timestamp: Timestamp = Timestamp.now()
    ) : FeedItem(userName = users, timestamp = timestamp, type = "CHALLENGE_START")

    data class ChallengeSuccess(
        val users: String = "",
        val description: String = "",
        override val timestamp: Timestamp = Timestamp.now()
    ) : FeedItem(userName = users, timestamp = timestamp, type = "CHALLENGE_SUCCESS")

    data class ReadingProgress(
        val user: String = "",
        val bookTitle: String = "",
        val currentPage: Int = 0,
        val totalPages: Int = 0,
        val description: String = "",
        override val timestamp: Timestamp = Timestamp.now()
    ) : FeedItem(userName = user, timestamp = timestamp, type = "READING_PROGRESS")

    data class BookReview(
        val user: String = "",
        val bookTitle: String = "",
        val rating: Int = 0,
        val review: String = "",
        override val timestamp: Timestamp = Timestamp.now()
    ) : FeedItem(userName = user, timestamp = timestamp, type = "BOOK_REVIEW")
}

fun formatTimestamp(timestamp: Timestamp): String {
    val diff = (System.currentTimeMillis() - timestamp.toDate().time) / 1000
    return when {
        diff < 60 -> "방금 전"
        diff < 3600 -> "${diff / 60}분 전"
        diff < 86400 -> "${diff / 3600}시간 전"
        else -> SimpleDateFormat("M월 d일", Locale.getDefault()).format(timestamp.toDate())
    }
}
