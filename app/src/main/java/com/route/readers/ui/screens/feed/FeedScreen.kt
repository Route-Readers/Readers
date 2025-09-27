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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.route.readers.ui.components.BottomNavBar
import com.route.readers.ui.components.BottomNavItem
import com.route.readers.ui.screens.community.CommunityScreen
import com.route.readers.ui.screens.mylibrary.MyLibraryScreen
import com.route.readers.ui.screens.profile.ProfileScreen
import com.route.readers.ui.screens.search.SearchScreen
import com.route.readers.ui.theme.*

@Composable
fun ActualFeedContent() {
    val feedItems = listOf(
        FeedItem.Follow("김독서님이 김이삭님을 팔로우했어요!", "김독서", "김이삭", "1분 전"),
        FeedItem.ChallengeStart("김이삭님과 김독서님", "친구와 10일동안 100페이지 읽기 챌린지를 시작하셨어요!", "2분 전"),
        FeedItem.ReadingProgress("김독서", "데미안", 45, 200, "오늘 30분 독서했어요", "1시간 전"),
        FeedItem.ChallengeSuccess("김이삭님과 김독서님", "1주일동안 매일 책읽기 챌린지를 성공하셨습니다!", "3시간 전"),
        FeedItem.BookReview("이책좋아", "해리포터와 마법사의 돌", 5, "정말 재미있었어요! 마법의 세계가 생생했습니다.", "5시간 전")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        Text("👤", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkRed
                    )
                }
                Text(
                    text = item.timeAgo,
                    color = TextGray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (item) {
                is FeedItem.Follow -> {
                    Text(text = item.description, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
                    ) {
                        Text("맞팔하기", color = White)
                    }
                }
                is FeedItem.ChallengeStart -> {
                    Text(text = item.description, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = 0f,
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkRed,
                        trackColor = White
                    )
                    Text("0% 완료", fontSize = 12.sp, color = TextGray)
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
                    LinearProgressIndicator(
                        progress = item.currentPage.toFloat() / item.totalPages,
                        modifier = Modifier.fillMaxWidth(),
                        color = ReadingGreen,
                        trackColor = White
                    )
                    Text("${item.currentPage}/${item.totalPages}페이지", fontSize = 12.sp, color = TextGray)
                }
                is FeedItem.BookReview -> {
                    Text(text = "📚 ${item.bookTitle}", fontWeight = FontWeight.Medium, color = DarkRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Text(
                                text = if (index < item.rating) "⭐" else "☆",
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${item.rating}/5", fontSize = 12.sp, color = TextGray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.review, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var isLiked by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = { isLiked = !isLiked }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "좋아요",
                            tint = if (isLiked) DarkRed else TextGray
                        )
                    }
                    Text("12", fontSize = 14.sp, color = TextGray)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    TextButton(onClick = { }) {
                        Text("💬 3", fontSize = 14.sp, color = TextGray)
                    }
                }
            }
        }
    }
}

sealed class FeedItem(val userName: String, val timeAgo: String) {
    data class Follow(
        val description: String,
        val follower: String,
        val following: String,
        val time: String
    ) : FeedItem(follower, time)

    data class ChallengeStart(
        val users: String,
        val description: String,
        val time: String
    ) : FeedItem(users, time)

    data class ChallengeSuccess(
        val users: String,
        val description: String,
        val time: String
    ) : FeedItem(users, time)

    data class ReadingProgress(
        val user: String,
        val bookTitle: String,
        val currentPage: Int,
        val totalPages: Int,
        val description: String,
        val time: String
    ) : FeedItem(user, time)

    data class BookReview(
        val user: String,
        val bookTitle: String,
        val rating: Int,
        val review: String,
        val time: String
    ) : FeedItem(user, time)
}

@Composable
fun FeedScreen() {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController = bottomNavController) }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Feed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Feed.route) {
                ActualFeedContent()
            }
            composable(BottomNavItem.MyLibrary.route) {
                MyLibraryScreen()
            }
            composable(BottomNavItem.Search.route) {
                SearchScreen()
            }
            composable(BottomNavItem.Community.route) {
                CommunityScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

