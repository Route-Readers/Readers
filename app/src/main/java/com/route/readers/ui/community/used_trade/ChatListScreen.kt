package com.route.readers.ui.community.used_trade

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    val chatList by viewModel.chatList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("채팅 목록") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (chatList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("채팅이 없습니다.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chatList) { chatItem ->
                        ChatListItemComposable(
                            chatItem = chatItem,
                            onClick = { otherUserId, bookId ->
                                onNavigateToChat(otherUserId, bookId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItemComposable(
    chatItem: ChatListItem,
    onClick: (String, String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MM.dd HH:mm", Locale.getDefault())
    val timeString = dateFormat.format(Date(chatItem.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick(chatItem.otherUserId, chatItem.bookId) },
        shape = MaterialTheme.shapes.medium, // Use the new rounded shape
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Use surface for cards (white)
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Add subtle shadow
    ) {
        Row(
            modifier = Modifier.padding(16.dp), // Increased inner padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = chatItem.otherUserProfileImageUrl,
                contentDescription = chatItem.otherUserName,
                modifier = Modifier
                    .size(52.dp) // Slightly larger profile image
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp)) // Increased spacing

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chatItem.otherUserName,
                        style = MaterialTheme.typography.titleMedium, // Use titleMedium
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall, // Use labelSmall
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Consistent color
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chatItem.lastMessage,
                    style = MaterialTheme.typography.bodyMedium, // Use bodyMedium
                    color = MaterialTheme.colorScheme.onSurface, // Darker for message
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                chatItem.bookTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall, // Use labelSmall
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // Consistent color
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}