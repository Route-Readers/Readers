package com.route.readers.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.route.readers.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.route.readers.data.model.User
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.route.readers.ui.theme.DarkRed
import com.route.readers.ui.theme.TextGray
import com.route.readers.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToAddFeed: () -> Unit,
    onNavigateToOtherUserProfile: (String) -> Unit,
    onFollowBack: (String) -> Unit,
    feedViewModel: FeedViewModel = viewModel()
) {
    val uiState by feedViewModel.uiState.collectAsState()
    val isRefreshing by feedViewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    // 화면이 보일 때마다 사용자 프로필 새로고침
    androidx.compose.runtime.LaunchedEffect(Unit) {
        feedViewModel.refreshUserProfiles()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddFeed, containerColor = DarkRed) {
                Icon(Icons.Default.Add, contentDescription = "피드 추가", tint = White)
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { feedViewModel.refreshFeeds() }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is FeedUiState.Loading -> CircularProgressIndicator()
                    is FeedUiState.Error -> Text(text = state.message)
                    is FeedUiState.Success -> {
                        if (state.items.isEmpty()) {
                            Text("표시할 피드가 없습니다.")
                        } else {
                            ActualFeedContent(
                                feedItems = state.items,
                                likedFeedIds = state.likedFeedIds,
                                savedFeedIds = state.savedFeedIds,
                                onLikeClick = { feedId, isLiked ->
                                    feedViewModel.toggleLike(feedId, isLiked)
                                },
                                onSaveClick = { feedId, isSaved ->
                                    feedViewModel.toggleSave(feedId, isSaved)
                                },
                                onDeleteFeed = { feedId ->
                                    feedViewModel.deleteFeed(feedId)
                                },
                                onNavigateToOtherUserProfile = onNavigateToOtherUserProfile,


                                onFollowBack = { followerId ->
                                    feedViewModel.followBack(followerId)
                                },
                                followerInfoMap = state.followerInfoMap
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActualFeedContent(
    feedItems: List<FeedItem>,
    likedFeedIds: Set<String>,
    savedFeedIds: Set<String>,
    onLikeClick: (String, Boolean) -> Unit,
    onSaveClick: (String, Boolean) -> Unit,
    onDeleteFeed: (String) -> Unit,
    onNavigateToOtherUserProfile: (String) -> Unit,
    onFollowBack: (String) -> Unit,
    followerInfoMap: Map<String, User> = emptyMap(),
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var feedToDelete by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog && feedToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                feedToDelete = null
            },
            title = { Text("피드 삭제") },
            text = { Text("피드를 정말 지우시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feedToDelete?.let { onDeleteFeed(it) }
                        showDeleteDialog = false
                        feedToDelete = null
                    }
                ) {
                    Text("예", color = DarkRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        feedToDelete = null
                    }
                ) {
                    Text("아니요", color = TextGray)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(feedItems, key = { it.id }) { item ->
            val isLiked = likedFeedIds.contains(item.id)
            val isSaved = savedFeedIds.contains(item.id)
            FeedCard(
                item = item,
                isLiked = isLiked,
                isSaved = isSaved,
                onLikeClick = { onLikeClick(item.id, isLiked) },
                onSaveClick = { onSaveClick(item.id, isSaved) },
                onDeleteClick = {
                    feedToDelete = item.id
                    showDeleteDialog = true
                },
                onUserClick = {
                    if (item is FeedItem.BookReview) {
                        onNavigateToOtherUserProfile(item.authorId)
                    }
                    if (item is FeedItem.FollowNotification) {
                        onNavigateToOtherUserProfile(item.followerId)

                    }
                },
                onFollowBack = onFollowBack,
                followerInfoMap = followerInfoMap,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun FeedCard(
    item: FeedItem,
    isLiked: Boolean,
    isSaved: Boolean,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUserClick: () -> Unit,
    onFollowBack: (String) -> Unit = {},
    followerInfoMap: Map<String, User> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Card(
        modifier = modifier.fillMaxWidth(),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable(onClick = onUserClick)
                ) {
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
                    Column {
                        Text(text = item.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkRed)
                        Text(text = formatTimestamp(item.timestamp), color = TextGray, fontSize = 12.sp)
                    }
                }

                IconButton(onClick = onSaveClick) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "저장",
                        tint = if (isSaved) DarkRed else TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (item) {
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
                is FeedItem.FollowNotification -> {
                    val followerName = followerInfoMap[item.followerId]?.nickname ?: "알 수 없는 사용자"
                    Text(
                        text = "👥 ${followerName}님이 팔로우했습니다!",
                        fontWeight = FontWeight.Medium,
                        color = DarkRed,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onFollowBack(item.followerId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.isFollowedBack) DarkRed else Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (item.isFollowedBack) "맞팔 완료" else "맞팔하기",
                            color = White
                        )
                    }
                }            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "좋아요",
                            tint = if (isLiked) DarkRed else TextGray
                        )
                    }
                    Text(text = item.likeCount.toString(), fontSize = 14.sp, color = TextGray)
                }

                Spacer(modifier = Modifier.weight(1f))

                val canDelete = when (item) {
                    is FeedItem.BookReview -> item.authorId == currentUserId
                    is FeedItem.FollowNotification -> item.receiverId == currentUserId
                }
                if (canDelete) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = TextGray
                        )
                    }
                }
            }
        }
    }
}


sealed class FeedItem(
    open val id: String = "",
    open val userName: String = "",
    open val timestamp: Timestamp = Timestamp.now(),
    open val likeCount: Int = 0,
    open val commentCount: Int = 0,
    open val type: String = ""
) {
    data class BookReview(
        override val id: String = "",
        val authorId: String = "",
        override val userName: String = "",
        val bookTitle: String = "",
        val review: String = "",
        val rating: Int = 0,
        val likedBy: List<String> = emptyList(),
        override val timestamp: Timestamp = Timestamp.now(),
        override val likeCount: Int = 0,
        override val commentCount: Int = 0
    ) : FeedItem(id, userName, timestamp, likeCount, commentCount, "BOOK_REVIEW")
    data class FollowNotification(
        override val id: String = "",
        val authorId: String = "",
        override val userName: String = "",
        val followerId: String = "",
        val receiverId: String = "",
        val isFollowedBack: Boolean = false,
        override val timestamp: Timestamp = Timestamp.now(),
        override val likeCount: Int = 0,
        override val commentCount: Int = 0
    ) : FeedItem(id, userName, timestamp, likeCount, commentCount, "FOLLOW_NOTIFICATION")}

fun formatTimestamp(timestamp: Timestamp): String {
    val diff = (System.currentTimeMillis() - timestamp.toDate().time) / 1000
    return when {
        diff < 60 -> "방금 전"
        diff < 3600 -> "${diff / 60}분 전"
        diff < 86400 -> "${diff / 3600}시간 전"
        else -> SimpleDateFormat("M월 d일", Locale.getDefault()).format(timestamp.toDate())
    }
}

fun DocumentSnapshot.toFeedItem(): FeedItem? {
    val type = getString("type") ?: return null
    return when (type) {
        "BOOK_REVIEW" -> this.toObject(FeedItem.BookReview::class.java)
        "FOLLOW_NOTIFICATION" -> this.toObject(FeedItem.FollowNotification::class.java)
        else -> null
    }
@Composable
fun ProfileImageInFeed(user: User?, userName: String) {
    val backgroundColor = try {
        user?.profileBackgroundColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: DarkRed
    } catch (e: Exception) {
        DarkRed
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            !user?.profileImageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user?.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "프로필 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            user?.profileCharacter != null -> {
                val drawableRes = when (user.profileCharacter) {
                    "lion" -> R.drawable.lion
                    "penguin" -> R.drawable.penguin
                    "redpanda" -> R.drawable.redpanda
                    "squirrel" -> R.drawable.squirrel
                    else -> null
                }
                
                drawableRes?.let {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .build(),
                        contentDescription = "캐릭터",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            else -> {
                Text(
                    text = userName.firstOrNull()?.toString() ?: "R",
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}}
