package com.route.readers.ui.screens.feed

import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.route.readers.ui.components.NativeAdComposable
import com.route.readers.ui.components.UserProfileImage
import com.route.readers.ui.screens.attendance.AttendanceViewModel
import com.route.readers.ui.theme.DarkRed
import com.route.readers.ui.theme.ReadingGreen
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
    feedViewModel: FeedViewModel = viewModel(),
    attendanceViewModel: AttendanceViewModel
) {
    val uiState by feedViewModel.uiState.collectAsState()
    val isRefreshing by feedViewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        attendanceViewModel.checkAttendance()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { feedViewModel.refreshFeeds() }
        ) {
            when (val state = uiState) {
                is FeedUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FeedUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is FeedUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("표시할 피드가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        SortableFeedContent(
                            feedItems = state.items,
                            likedFeedIds = state.likedFeedIds,
                            bookmarkedFeedIds = state.bookmarkedFeedIds,
                            onLikeClick = { feedId, isLiked ->
                                feedViewModel.toggleLike(feedId, isLiked)
                            },
                            onBookmarkClick = { feedId, isBookmarked ->
                                feedViewModel.toggleBookmark(feedId, isBookmarked)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                Toast.makeText(
                                    context,
                                    if (isBookmarked) "북마크에서 삭제했습니다." else "북마크에 추가했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onDeleteFeed = { feedId: String ->
                                feedViewModel.deleteFeed(feedId)
                            },
                            onNavigateToOtherUserProfile = onNavigateToOtherUserProfile,
                            onFollowBack = { followerId ->
                                feedViewModel.followBack(followerId)
                            },
                            followerInfoMap = state.followerInfoMap,
                            wishlist = state.wishlist,
                            myLibrary = state.myLibrary,
                            onToggleWishlist = { book, isInWishlist ->
                                feedViewModel.toggleWishlist(book, isInWishlist)
                                Toast.makeText(
                                    context,
                                    if (isInWishlist) "관심도서에서 삭제했습니다." else "관심도서에 추가했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onToggleMyLibrary = { book, isInMyLibrary ->
                                feedViewModel.toggleMyLibrary(book, isInMyLibrary)
                                Toast.makeText(
                                    context,
                                    if (isInMyLibrary) "서재에서 삭제했습니다." else "서재에 추가했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNavigateToAddFeed,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "피드 추가")
        }
    }
}

@Composable
fun SortableFeedContent(
    feedItems: List<FeedItem>,
    likedFeedIds: Set<String>,
    bookmarkedFeedIds: Set<String>,
    onLikeClick: (String, Boolean) -> Unit,
    onBookmarkClick: (String, Boolean) -> Unit,
    onDeleteFeed: (String) -> Unit,
    onNavigateToOtherUserProfile: (String) -> Unit,
    onFollowBack: (String) -> Unit,
    followerInfoMap: Map<String, User>,
    wishlist: List<String>,
    myLibrary: List<String>,
    onToggleWishlist: (Book, Boolean) -> Unit,
    onToggleMyLibrary: (Book, Boolean) -> Unit
) {
    var sortOption by remember { mutableStateOf(SortOption.LATEST) }
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
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = sortOption.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable {
                        sortOption = if (sortOption == SortOption.LATEST) {
                            SortOption.POPULAR
                        } else {
                            SortOption.LATEST
                        }
                    }
                    .padding(vertical = 12.dp)
            )
        }

        ActualFeedContent(
            listState = listState,
            feedItems = sortedFeedItems,
            likedFeedIds = likedFeedIds,
            bookmarkedFeedIds = bookmarkedFeedIds,
            onLikeClick = onLikeClick,
            onBookmarkClick = onBookmarkClick,
            onDeleteFeed = onDeleteFeed,
            onNavigateToOtherUserProfile = onNavigateToOtherUserProfile,
            onFollowBack = onFollowBack,
            followerInfoMap = followerInfoMap,
            wishlist = wishlist,
            myLibrary = myLibrary,
            onToggleWishlist = onToggleWishlist,
            onToggleMyLibrary = onToggleMyLibrary
        )
    }
}


@Composable
fun ActualFeedContent(
    listState: LazyListState,
    feedItems: List<FeedItem>,
    likedFeedIds: Set<String>,
    bookmarkedFeedIds: Set<String>,
    onLikeClick: (String, Boolean) -> Unit,
    onBookmarkClick: (String, Boolean) -> Unit,
    onDeleteFeed: (String) -> Unit,
    onNavigateToOtherUserProfile: (String) -> Unit,
    onFollowBack: (String) -> Unit,
    followerInfoMap: Map<String, User> = emptyMap(),
    wishlist: List<String>,
    myLibrary: List<String>,
    onToggleWishlist: (Book, Boolean) -> Unit,
    onToggleMyLibrary: (Book, Boolean) -> Unit
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
                    Text("예", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        feedToDelete = null
                    }
                ) {
                    Text("아니요", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(feedItems, key = { _, item -> item.id }) { index, item ->
            val isLiked = likedFeedIds.contains(item.id)
            val isBookmarked = bookmarkedFeedIds.contains(item.id)
            FeedCard(
                item = item,
                isLiked = isLiked,
                isBookmarked = isBookmarked,
                onLikeClick = { onLikeClick(item.id, isLiked) },
                onBookmarkClick = { onBookmarkClick(item.id, isBookmarked) },
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
                modifier = Modifier,
                wishlist = wishlist,
                myLibrary = myLibrary,
                onToggleWishlist = onToggleWishlist,
                onToggleMyLibrary = onToggleMyLibrary
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFE0E0E0)
            )

            if ((index + 1) % 3 == 0) {
                NativeAdComposable()
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0xFFE0E0E0)
                )
            }
        }
    }
}

@Composable
fun FeedCard(
    item: FeedItem,
    isLiked: Boolean,
    isBookmarked: Boolean,
    onLikeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUserClick: () -> Unit,
    onFollowBack: (String) -> Unit = {},
    followerInfoMap: Map<String, User> = emptyMap(),
    modifier: Modifier = Modifier,
    wishlist: List<String>,
    myLibrary: List<String>,
    onToggleWishlist: (Book, Boolean) -> Unit,
    onToggleMyLibrary: (Book, Boolean) -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var isBookCardExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
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
                        UserProfileImage(user = userInfo, size = 40.dp, fontSize = 20.sp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.userName.firstOrNull()?.toString() ?: "R",
                                color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = item.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            userInfo?.title?.let { title ->
                                Text(
                                    text = " $title",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        Text(text = formatTimestamp(item.timestamp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "저장",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (item) {
                is FeedItem.BookReview -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AnimatedVisibility(
                            visible = isBookCardExpanded,
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                        ) {
                            item.book?.let { book ->
                                Column {
                                    val isInWishlist = wishlist.contains(book.isbn)
                                    val isInMyLibrary = myLibrary.contains(book.isbn)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Progress bar section first
                                    Column {
                                        val progressPercentage = item.progress
                                        val progressColor = if (progressPercentage == 100) MaterialTheme.colorScheme.primary else ReadingGreen

                                        Text(
                                            text = if (progressPercentage == 100) "완독!" else "$progressPercentage%",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = progressColor
                                        )
                                        LinearProgressIndicator(
                                            progress = { progressPercentage / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = progressColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "이번에 읽은 양: ${item.currentPage} 페이지",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Then the book card with buttons
                                    SelectedBookCard(
                                        book = book,
                                        onClear = null,
                                        isInWishlist = isInWishlist,
                                        isInMyLibrary = isInMyLibrary,
                                        onToggleWishlist = { onToggleWishlist(book, isInWishlist) },
                                        onToggleMyLibrary = { onToggleMyLibrary(book, isInMyLibrary) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isBookCardExpanded = !isBookCardExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📖 ${item.book?.title ?: item.bookTitle}",
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isBookCardExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isBookCardExpanded) "접기" else "펼치기", tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val starColor = Color(0xFFFFD700)
                            repeat(5) { index ->
                                Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = if (index < item.rating) starColor else MaterialTheme.colorScheme.surfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${item.rating}/5", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item.review, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, bottom = 12.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onLikeClick)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "좋아요",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = item.likeCount.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (item.authorId == currentUserId) {
                            IconButton(onClick = onDeleteClick) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                is FeedItem.FollowNotification -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val followerName = followerInfoMap[item.followerId]?.nickname ?: "알 수 없는 사용자"
                        Text(
                            text = "👥 ${followerName}님이 팔로우했습니다!",
                            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onFollowBack(item.followerId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.isFollowedBack) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (item.isFollowedBack) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (item.isFollowedBack) "팔로잉" else "맞팔하기")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(onClick = onLikeClick)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "좋아요",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = item.likeCount.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (item.receiverId == currentUserId) {
                                IconButton(onClick = onDeleteClick) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedBookCard(
    book: Book,
    onClear: (() -> Unit)? = null,
    isInWishlist: Boolean,
    isInMyLibrary: Boolean,
    onToggleWishlist: () -> Unit,
    onToggleMyLibrary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(0.dp)
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
                    Text(book.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                if (onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "선택 취소", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleWishlist,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInWishlist) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(if (isInWishlist) "관심도서에서 삭제" else "관심도서 추가")
                }

                OutlinedButton(
                    onClick = onToggleMyLibrary,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isInMyLibrary) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(if (isInMyLibrary) "서재에서 삭제" else "서재에 추가")
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
    open val type: String = "",
    open val isBookmarked: Boolean = false
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
        val bookmarkedBy: List<String> = emptyList(),
        override val timestamp: Timestamp = Timestamp.now(),
        override val likeCount: Int = 0,
        override val commentCount: Int = 0,
        override val isBookmarked: Boolean = false
    ) : FeedItem(id, userName, timestamp, likeCount, commentCount, "BOOK_REVIEW", isBookmarked)

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
                id = this.id,
                book = book,
                bookmarkedBy = get("bookmarkedBy") as? List<String> ?: emptyList()
            )

            bookReview?.copy(bookTitle = book?.title ?: bookReview.bookTitle)
        }
        "FOLLOW_NOTIFICATION" -> {
            val notification = this.toObject(FeedItem.FollowNotification::class.java)
            notification?.copy(
                id = this.id,
                timestamp = getTimestamp("timestamp") ?: Timestamp.now()
            )
        }
        else -> null
    }
}
