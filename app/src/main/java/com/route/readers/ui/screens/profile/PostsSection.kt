package com.route.readers.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.route.readers.ui.screens.feed.FeedCard
import com.route.readers.ui.screens.feed.FeedItem

@Composable
fun PostsSection(
    myPosts: List<FeedItem.BookReview>,
    savedPosts: List<FeedItem.BookReview>,
    onDeletePost: (String) -> Unit = {},
    onToggleSavePost: (FeedItem.BookReview) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("내 게시물") }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { selectedTab = "내 게시물" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (selectedTab == "내 게시물")
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("내 게시물 (${myPosts.size})")
            }

            TextButton(
                onClick = { selectedTab = "저장된 게시물" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (selectedTab == "저장된 게시물")
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("저장된 게시물 (${savedPosts.size})")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            "내 게시물" -> {
                if (myPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "작성한 게시물이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        myPosts.forEach { post ->
                            FeedCard(
                                item = post,
                                isLiked = false, // Not available in this context
                                isBookmarked = savedPosts.any { it.id == post.id }, // Check if the post is in saved list
                                onLikeClick = { /* Not available */ },
                                onBookmarkClick = { onToggleSavePost(post) },
                                onDeleteClick = { onDeletePost(post.id) },
                                onUserClick = { /* Not available */ }
                            )
                        }
                    }
                }
            }

            "저장된 게시물" -> {
                if (savedPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "저장된 게시물이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        savedPosts.forEach { post ->
                            FeedCard(
                                item = post,
                                isLiked = false, // Not available in this context
                                isBookmarked = true, // Always true for saved posts
                                onLikeClick = { /* Not available */ },
                                onBookmarkClick = { onToggleSavePost(post) },
                                onDeleteClick = { /* Not applicable for saved posts */ },
                                onUserClick = { /* Not available */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
