package com.route.readers.ui.screens.bookclub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookClubChatScreen(
    bookClubId: String,
    bookClubName: String,
    onBackClick: () -> Unit,
    viewModel: BookClubChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    LaunchedEffect(bookClubId) {
        viewModel.loadMessages(bookClubId)
        focusRequester.requestFocus()
    }
    
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bookClubName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.retryLoadMessages(bookClubId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    // 오류 메시지 표시
                    uiState.error?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                TextButton(onClick = { viewModel.clearError() }) {
                                    Text("닫기")
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text("메시지를 입력하세요...") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            enabled = !uiState.isSending,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.None)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (messageText.isNotBlank() && !uiState.isSending) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (messageText.isNotBlank() && !uiState.isSending) {
                                        val msg = messageText.trim()
                                        messageText = ""
                                        viewModel.sendMessage(bookClubId, msg)
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "전송",
                                    tint = if (messageText.isNotBlank()) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "메시지를 불러오는 중...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            uiState.messages.isEmpty() && !uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "아직 메시지가 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "첫 번째 메시지를 보내보세요!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                val koreaTimeZone = remember { TimeZone.getTimeZone("Asia/Seoul") }
                val dateFormat = remember {
                    SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA).apply {
                        timeZone = koreaTimeZone
                    }
                }
                val dayFormat = remember {
                    SimpleDateFormat("yyyyMMdd", Locale.KOREA).apply {
                        timeZone = koreaTimeZone
                    }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.messages.forEachIndexed { index, message ->
                        val messageDate = dayFormat.format(Date(message.timestamp))
                        val prevMessageDate = if (index > 0) {
                            dayFormat.format(Date(uiState.messages[index - 1].timestamp))
                        } else null
                        
                        // 첫 메시지이거나 날짜가 바뀌면 날짜 구분선 표시
                        if (prevMessageDate != messageDate) {
                            item(key = "date_$messageDate") {
                                DateDivider(dateFormat.format(Date(message.timestamp)))
                            }
                        }
                        item(key = "msg_${message.id}_$index") {
                            ChatMessageItem(message = message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateDivider(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isCurrentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == message.senderId
    val timeFormat = remember {
        SimpleDateFormat("a h:mm", Locale.KOREA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        // 상대방일 때만 프로필 이미지 표시
        if (!isCurrentUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (message.senderProfileImage.isNotEmpty()) {
                    AsyncImage(
                        model = message.senderProfileImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (!isCurrentUser) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isCurrentUser) 16.dp else 4.dp,
                    topEnd = if (isCurrentUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = message.message,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
