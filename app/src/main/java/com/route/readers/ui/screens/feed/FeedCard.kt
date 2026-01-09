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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.Book
import com.route.readers.data.model.User
import com.route.readers.ui.components.UserProfileImage
import com.route.readers.ui.theme.ReadingGreen
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FeedCard(
    item: FeedItem,
    isLiked: Boolean,
    isBookmarked: Boolean,
    onLikeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUserClick: () -> Unit,
    onReportClick: () -> Unit = {},
    onFollowBack: (String) -> Unit = {},
    followerInfoMap: Map<String, User> = emptyMap(),
    modifier: Modifier = Modifier,
    wishlist: List<String> = emptyList(),
    myLibrary: List<String> = emptyList(),
    onToggleWishlist: (Book, Boolean) -> Unit = { _, _ -> },
    onToggleMyLibrary: (Book, Boolean) -> Unit = { _, _ -> }
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var isBookCardExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val feedAuthorId = when (item) {
        is FeedItem.BookReview -> item.authorId
        is FeedItem.FollowNotification -> item.followerId
    }
    val isMyPost = currentUserId == feedAuthorId

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Add vertical spacing between cards
        shape = MaterialTheme.shapes.medium, // Use the new rounded shape
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Add slight shadow
    ) {
        Column(
            modifier = Modifier.padding(bottom = 12.dp) // Add padding at the bottom of the card
        ) {
            // Header: User Info & Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onUserClick)
                ) {
                    val userIdForProfile = when (item) {
                        is FeedItem.BookReview -> item.authorId
                        is FeedItem.FollowNotification -> item.followerId
                    }
                    val userInfo = followerInfoMap[userIdForProfile]

                    if (userInfo != null) {
                        UserProfileImage(user = userInfo, size = 44.dp, fontSize = 20.sp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.userName.firstOrNull()?.toString() ?: "R",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.userName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            userInfo?.title?.let { title ->
                                Text(
                                    text = " • $title",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = formatTimestamp(item.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onBookmarkClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "저장",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 더보기 메뉴 (신고/삭제)
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (isMyPost) {
                            DropdownMenuItem(
                                text = { Text("삭제") },
                                onClick = { showMenu = false; onDeleteClick() },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("신고하기") },
                                onClick = { showMenu = false; onReportClick() },
                                leadingIcon = { Icon(Icons.Default.Flag, null) }
                            )
                        }
                    }
                }
            }

            when (item) {
                is FeedItem.BookReview -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Collapsible Header (Book Title)
                        Surface(
                            onClick = { isBookCardExpanded = !isBookCardExpanded },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // Neutral gray/brown tint
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook, // Changed to MenuBook icon
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.book?.title ?: item.bookTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isBookCardExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isBookCardExpanded) "접기" else "펼치기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Book Info (Collapsible)
                        AnimatedVisibility(
                            visible = isBookCardExpanded,
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                        ) {
                            item.book?.let { book ->
                                Column {
                                    val isInWishlist = wishlist.contains(book.isbn)
                                    val isInMyLibrary = myLibrary.contains(book.isbn)
                                    
                                    // Reading Progress
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp) // Adjusted padding
                                    ) {
                                        val progressPercentage = item.progress
                                        val progressColor = if (progressPercentage == 100) MaterialTheme.colorScheme.primary else ReadingGreen
                                        
                                        CircularProgressIndicator(
                                            progress = { progressPercentage / 100f },
                                            modifier = Modifier.size(40.dp),
                                            color = progressColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (progressPercentage == 100) "완독했어요! 🎉" else "$progressPercentage% 읽었어요",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = progressColor
                                            )
                                            Text(
                                                text = "이번 기록: ${item.currentPage} 페이지",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    SelectedBookCard(
                                        book = book,
                                        onClear = null,
                                        isInWishlist = isInWishlist,
                                        isInMyLibrary = isInMyLibrary,
                                        onToggleWishlist = { onToggleWishlist(book, isInWishlist) },
                                        onToggleMyLibrary = { onToggleMyLibrary(book, isInMyLibrary) }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val starColor = Color(0xFFFFD700)
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (index < item.rating) starColor else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.rating}.0",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        // Review Content
                        Text(
                            text = item.review,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Footer: Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onLikeClick) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "좋아요",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.likeCount.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (item.authorId == currentUserId) {
                            IconButton(onClick = onDeleteClick) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                // Handle other feed types like Notification...
                is FeedItem.FollowNotification -> {
                     Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val followerName = followerInfoMap[item.followerId]?.nickname ?: "알 수 없는 사용자"
                        Text(
                            text = "👋 ${followerName}님이 팔로우를 시작했어요!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onFollowBack(item.followerId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.isFollowedBack) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary,
                                contentColor = if (item.isFollowedBack) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(text = if (item.isFollowedBack) "팔로잉" else "맞팔로우 하기")
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
                    Text(if (isInMyLibrary) "서재에 추가" else "서재에 추가")
                }
            }
        }
    }
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
