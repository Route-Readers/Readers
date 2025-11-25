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
                            PostItem(
                                post = post,
                                isMyPost = true,
                                onDelete = { onDeletePost(post.id) }
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
                            PostItem(
                                post = post,
                                isMyPost = false,
                                onToggleSave = { onToggleSavePost(post) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostItem(
    post: FeedItem.BookReview,
    isMyPost: Boolean,
    onDelete: () -> Unit = {},
    onToggleSave: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = post.bookTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = post.review,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "평점: ${post.rating}/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (isMyPost) {
                    TextButton(onClick = onDelete) {
                        Text("삭제")
                    }
                } else {
                    TextButton(onClick = onToggleSave) {
                        Text("저장 해제")
                    }
                }
            }
        }
    }
}
