package com.route.readers.ui.screens.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.route.readers.data.model.Book
import com.route.readers.data.model.User
import com.route.readers.ui.components.UserProfileImage
import com.route.readers.ui.theme.DarkRed
import com.route.readers.ui.theme.ReadingGreen
import com.route.readers.ui.theme.TextGray
import com.route.readers.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Locale

enum class SortOption(val displayName: String) {
    LATEST("최신순"),
    POPULAR("인기순")
}

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
                            SortableFeedContent(
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
fun SortableFeedContent(
    feedItems: List<FeedItem>,
    likedFeedIds: Set<String>,
    savedFeedIds: Set<String>,
    onLikeClick: (String, Boolean) -> Unit,
    onSaveClick: (String, Boolean) -> Unit,
    onDeleteFeed: (String) -> Unit,
    onNavigateToOtherUserProfile: (String) -> Unit,
    onFollowBack: (String) -> Unit,
    followerInfoMap: Map<String, User>,
) {
    var sortOption by remember { mutableStateOf(SortOption.LATEST) }
    var menuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val sortedFeedItems = remember(feedItems, sortOption) {
        when (sortOption) {
            SortOption.LATEST -> feedItems.sortedByDescending { it.timestamp }
            SortOption.POPULAR -> feedItems.sortedByDescending { it.likeCount }
        }
    }

    LaunchedEffect(sortOption) {
        listState.scrollToItem(index = 0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sortOption.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "정렬 메뉴",
                            tint = TextGray
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(SortOption.LATEST.displayName) },
                            onClick = {
                                sortOption = SortOption.LATEST
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(SortOption.POPULAR.displayName) },
                            onClick = {
                                sortOption = SortOption.POPULAR
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        ActualFeedContent(
            listState = listState,
            feedItems = sortedFeedItems,
            likedFeedIds = likedFeedIds,
            savedFeedIds = savedFeedIds,
            onLikeClick = onLikeClick,
            onSaveClick = onSaveClick,
            onDeleteFeed = onDeleteFeed,
            onNavigateToOtherUserProfile = onNavigateToOtherUserProfile,
            onFollowBack = onFollowBack,
            followerInfoMap = followerInfoMap
        )
    }
}


@Composable
fun ActualFeedContent(
    listState: LazyListState,
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
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
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
                    val userId = when (item) {
                        is FeedItem.BookReview -> item.authorId
                        is FeedItem.FollowNotification -> item.followerId
                    }
                    onNavigateToOtherUserProfile(userId)
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
    var isBookCardExpanded by remember { mutableStateOf(false) }

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
                    val userIdForProfile = when (item) {
                        is FeedItem.BookReview -> item.authorId
                        is FeedItem.FollowNotification -> item.followerId
                    }
                    val userInfo = followerInfoMap[userIdForProfile]

                    if (userInfo != null) {
                        UserProfileImage(
                            user = userInfo,
                            size = 40.dp,
                            fontSize = 20.sp
                        )
                    } else {
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
                    AnimatedVisibility(
                        visible = isBookCardExpanded,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                    ) {
                        item.book?.let { book ->
                            SelectedBookCard(book = book, onClear = null)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBookCardExpanded = !isBookCardExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📚 ${item.book?.title ?: item.bookTitle}",
                            fontWeight = FontWeight.Medium,
                            color = DarkRed,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isBookCardExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isBookCardExpanded) "접기" else "펼치기",
                            tint = TextGray
                        )
                    }

                    if (item.progress > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = item.progress / 100f,
                                modifier = Modifier.weight(1f).height(8.dp),
                                color = ReadingGreen,
                                trackColor = Color.LightGray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${item.progress}% (${item.currentPage}p)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReadingGreen
                            )
                        }
                    }

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
                }
            }

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

@Composable
fun SelectedBookCard(book: Book, onClear: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = book.title,
                    modifier = Modifier
                        .height(120.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(book.author, color = Color.DarkGray, fontSize = 14.sp)
                }
                if (onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "선택 취소")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* TODO: 관심도서 추가 로직 */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkRed,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("관심도서 추가")
                }

                OutlinedButton(
                    onClick = { /* TODO: 서재에 추가 로직 */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkRed),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("서재에 추가", color = DarkRed)
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
        val book: Book? = null,
        val bookTitle: String = book?.title ?: "",
        val review: String = "",
        val rating: Int = 0,
        val currentPage: Int = 0,
        val progress: Int = 0,
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
    ) : FeedItem(id, userName, timestamp, likeCount, commentCount, "FOLLOW_NOTIFICATION")
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

fun DocumentSnapshot.toFeedItem(): FeedItem? {
    val type = getString("type") ?: return null
    return when (type) {
        "BOOK_REVIEW" -> {
            val bookData = get("book") as? Map<String, Any>
            val book = if (bookData != null) {
                Book(
                    title = bookData["title"] as? String ?: "",
                    author = bookData["author"] as? String ?: "",
                    description = bookData["description"] as? String ?: "",
                    isbn = bookData["isbn"] as? String ?: "",
                    cover = bookData["cover"] as? String ?: ""
                )
            } else {
                null
            }

            val bookReview = toObject(FeedItem.BookReview::class.java)?.copy(
                id = this.id, // 문서 ID를 명시적으로 설정
                book = book
            )

            // book 객체가 있으면 title을, 없으면 기존 bookTitle 필드를 사용
            bookReview?.copy(bookTitle = book?.title ?: bookReview.bookTitle)
        }
        "FOLLOW_NOTIFICATION" -> this.toObject(FeedItem.FollowNotification::class.java)?.copy(
            id = this.id // 문서 ID를 명시적으로 설정
        )
        else -> null
    }
}
