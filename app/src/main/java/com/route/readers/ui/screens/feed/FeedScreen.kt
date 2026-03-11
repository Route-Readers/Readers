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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import com.route.readers.R
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
import com.route.readers.ui.components.AdBanner
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
            .background(Color(0xFFF5F5F5)) // Very Light Gray
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AdBanner()
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
    var showReportDialog by remember { mutableStateOf(false) }
    var reportTargetFeed by remember { mutableStateOf<FeedItem?>(null) }

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

    if (showReportDialog && reportTargetFeed != null) {
        val feed = reportTargetFeed!!
        val targetOwnerId = when (feed) {
            is FeedItem.BookReview -> feed.authorId
            is FeedItem.FollowNotification -> feed.followerId
        }
        com.route.readers.ui.components.ReportDialog(
            targetId = feed.id,
            targetType = "feed",
            targetOwnerId = targetOwnerId,
            onDismiss = { showReportDialog = false; reportTargetFeed = null }
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
                onReportClick = {
                    reportTargetFeed = item
                    showReportDialog = true
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
