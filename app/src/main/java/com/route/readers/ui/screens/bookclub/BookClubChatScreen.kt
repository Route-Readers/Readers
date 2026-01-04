package com.route.readers.ui.screens.bookclub

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.BookClubRole
import com.route.readers.data.model.ChatMessage
import com.route.readers.data.model.User
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookClubChatScreen(
    bookClubId: String,
    bookClubName: String,
    onBackClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    viewModel: BookClubChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // 검색 관련 상태
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 달력 관련 상태
    var showDatePicker by remember { mutableStateOf(false) }
    
    // 책 정보 다이얼로그 상태
    var showBookInfoDialog by remember { mutableStateOf(false) }
    
    // 더보기 메뉴 상태
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    
    // 멤버 목록 다이얼로그 상태
    var showMemberListDialog by remember { mutableStateOf(false) }
    
    val koreaTimeZone = remember { TimeZone.getTimeZone("Asia/Seoul") }
    val dayFormat = remember {
        SimpleDateFormat("yyyyMMdd", Locale.KOREA).apply {
            timeZone = koreaTimeZone
        }
    }
    
    // 검색 결과 필터링
    val filteredMessages = remember(searchQuery, uiState.messages) {
        if (searchQuery.isBlank()) uiState.messages
        else uiState.messages.filter { it.message.contains(searchQuery, ignoreCase = true) }
    }
    
    // 시스템 뒤로가기 처리
    BackHandler {
        onBackClick()
    }
    
    LaunchedEffect(bookClubId) {
        viewModel.loadMessages(bookClubId)
        viewModel.loadBookClubInfo(bookClubId)
    }
    
    val scope = rememberCoroutineScope()
    
    // 권한 체크
    val isOwner = uiState.userRole == BookClubRole.OWNER
    val isViceOwner = uiState.userRole == BookClubRole.VICE_OWNER
    val canManage = isOwner || isViceOwner
    
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    // 날짜 선택 다이얼로그
    if (showDatePicker) {
        BookCalendarDialog(
            bookClub = uiState.bookClub,
            messages = uiState.messages,
            canEditSchedule = canManage,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                val index = uiState.messages.indexOfFirst { 
                    dayFormat.format(Date(it.timestamp)) == date 
                }
                if (index >= 0) {
                    scope.launch {
                        listState.animateScrollToItem(index)
                    }
                }
                showDatePicker = false
            },
            onSaveBookHistory = { dates ->
                uiState.bookClub?.currentBookCover?.let { cover ->
                    if (cover.isNotEmpty()) {
                        viewModel.saveBookHistoryForDates(bookClubId, dates, cover)
                    }
                }
            },
            onDeleteBookHistory = { dates ->
                viewModel.deleteBookHistoryForDates(bookClubId, dates)
            }
        )
    }
    
    // 책 정보 다이얼로그
    if (showBookInfoDialog) {
        BookInfoDialog(
            bookClub = uiState.bookClub,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearchingBooks,
            canChangeBook = canManage,
            onDismiss = { 
                showBookInfoDialog = false
                viewModel.clearSearchResults()
            },
            onSearch = { query -> viewModel.searchBooks(query) },
            onBookSelected = { book ->
                viewModel.updateBookClubBook(bookClubId, book)
                viewModel.clearSearchResults()
            }
        )
    }
    
    // 멤버 목록 다이얼로그
    if (showMemberListDialog) {
        MemberListDialog(
            bookClub = uiState.bookClub,
            memberProfiles = uiState.memberProfiles,
            currentUserRole = uiState.userRole,
            onDismiss = { showMemberListDialog = false },
            onProfileClick = { userId ->
                showMemberListDialog = false
                onNavigateToProfile(userId)
            },
            onKickMember = { memberId -> viewModel.kickMember(bookClubId, memberId) },
            onSetViceOwner = { memberId, isVice -> viewModel.setViceOwner(bookClubId, memberId, isVice) },
            getMemberRole = { viewModel.getMemberRole(it) }
        )
    }
    
    // 북클럽 삭제 확인 다이얼로그 (방장용)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("북클럽 삭제") },
            text = { Text("정말로 이 북클럽을 삭제하시겠습니까?\n모든 채팅 기록이 삭제됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBookClub(bookClubId) { onBackClick() }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("취소") }
            }
        )
    }
    
    // 북클럽 나가기 확인 다이얼로그 (멤버용)
    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text("북클럽 나가기") },
            text = { Text("정말로 이 북클럽을 나가시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveBookClub(bookClubId) { onBackClick() }
                        showLeaveConfirmDialog = false
                    }
                ) { Text("나가기") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) { Text("취소") }
            }
        )
    }
    
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSearchMode) {
                    // 검색 모드 TopAppBar
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("채팅 내용 검색...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSearchMode = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "검색 닫기")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    windowInsets = WindowInsets(0.dp)
                )
            } else {
                // 기본 TopAppBar
                TopAppBar(
                    title = { Text(bookClubName) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    actions = {
                        // 책 표지 이미지
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .width(28.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showBookInfoDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.bookClub?.currentBookCover?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = uiState.bookClub?.currentBookCover,
                                    contentDescription = "책 정보",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        IconButton(onClick = { isSearchMode = true }) {
                            Icon(Icons.Default.Search, contentDescription = "검색")
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "날짜 선택")
                        }
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "더보기")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    windowInsets = WindowInsets(0.dp)
                )
            }
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
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            // 터치 시작 시 즉시 포커스 유지
                                            focusRequester.requestFocus()
                                        },
                                        onTap = {
                                            if (messageText.isNotBlank() && !uiState.isSending) {
                                                val msg = messageText.trim()
                                                messageText = ""
                                                viewModel.sendMessage(bookClubId, msg)
                                            }
                                        }
                                    )
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
                val dateFormat = remember {
                    SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA).apply {
                        timeZone = koreaTimeZone
                    }
                }
                
                val displayMessages = if (isSearchMode && searchQuery.isNotBlank()) filteredMessages else uiState.messages
                
                if (isSearchMode && searchQuery.isNotBlank() && filteredMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "검색 결과가 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayMessages.forEachIndexed { index, message ->
                            val messageDate = dayFormat.format(Date(message.timestamp))
                            val prevMessageDate = if (index > 0) {
                                dayFormat.format(Date(displayMessages[index - 1].timestamp))
                            } else null
                            
                            // 첫 메시지이거나 날짜가 바뀌면 날짜 구분선 표시
                            if (prevMessageDate != messageDate) {
                                item(key = "date_${messageDate}_$index") {
                                    DateDivider(dateFormat.format(Date(message.timestamp)))
                                }
                            }
                            item(key = "msg_${message.id}_$index") {
                                ChatMessageItem(
                                    message = message,
                                    highlightText = if (isSearchMode) searchQuery else null,
                                    onProfileClick = onNavigateToProfile
                                )
                            }
                        }
                    }
                }
            }
        }
        }
        
        // 사이드 시트 (더보기 메뉴)
        AnimatedVisibility(
            visible = showMoreMenu,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 왼쪽 빈 공간 (클릭하면 닫힘)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { showMoreMenu = false }
                )
                // 오른쪽 사이드 시트 (2/3 차지)
                Surface(
                    modifier = Modifier
                        .width(screenWidth * 2 / 3)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // 책 표지
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { 
                                    showMoreMenu = false
                                    showBookInfoDialog = true 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.bookClub?.currentBookCover?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = uiState.bookClub?.currentBookCover,
                                    contentDescription = "책 표지",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("책 표지 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 방 제목
                        Text(
                            text = bookClubName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = uiState.bookClub?.currentBook ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        HorizontalDivider()
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 멤버 목록 헤더 (중복 제거: 방장 + 부방장 + 일반멤버)
                        val club = uiState.bookClub
                        val totalMembers = if (club != null) {
                            val allMembers = (listOf(club.createdBy) + club.members).distinct()
                            allMembers.size
                        } else 0
                        Text(
                            text = "참여 멤버 (${totalMembers}명)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 멤버 목록
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.bookClub?.let { club ->
                                // 방장
                                item {
                                    SideSheetMemberItem(
                                        user = uiState.memberProfiles[club.createdBy],
                                        role = BookClubRole.OWNER,
                                        isOwner = isOwner,
                                        canManage = false,
                                        onClick = {
                                            showMoreMenu = false
                                            onNavigateToProfile(club.createdBy)
                                        }
                                    )
                                }
                                // 부방장들
                                items(club.viceOwners.size) { index ->
                                    val memberId = club.viceOwners[index]
                                    SideSheetMemberItem(
                                        user = uiState.memberProfiles[memberId],
                                        role = BookClubRole.VICE_OWNER,
                                        isOwner = isOwner,
                                        canManage = isOwner,
                                        onClick = {
                                            showMoreMenu = false
                                            onNavigateToProfile(memberId)
                                        },
                                        onRemoveViceOwner = { viewModel.setViceOwner(bookClubId, memberId, false) },
                                        onKick = { viewModel.kickMember(bookClubId, memberId) }
                                    )
                                }
                                // 일반 멤버들
                                items(club.members.size) { index ->
                                    val memberId = club.members[index]
                                    if (!club.viceOwners.contains(memberId) && memberId != club.createdBy) {
                                        SideSheetMemberItem(
                                            user = uiState.memberProfiles[memberId],
                                            role = BookClubRole.MEMBER,
                                            isOwner = isOwner,
                                            canManage = canManage,
                                            onClick = {
                                                showMoreMenu = false
                                                onNavigateToProfile(memberId)
                                            },
                                            onSetViceOwner = { viewModel.setViceOwner(bookClubId, memberId, true) },
                                            onKick = { viewModel.kickMember(bookClubId, memberId) }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 나가기/삭제 아이콘
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isOwner) {
                                IconButton(
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "북클럽 삭제",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        showMoreMenu = false
                                        showLeaveConfirmDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = "북클럽 나가기",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
fun ChatMessageItem(
    message: ChatMessage,
    highlightText: String? = null,
    onProfileClick: (String) -> Unit = {}
) {
    // 시스템 메시지 처리
    if (message.senderId == "system") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = message.message,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onProfileClick(message.senderId) },
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

@Composable
fun BookInfoDialog(
    bookClub: com.route.readers.data.model.BookClub?,
    searchResults: List<com.route.readers.data.model.Book>,
    isSearching: Boolean,
    canChangeBook: Boolean = false,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onBookSelected: (com.route.readers.data.model.Book) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            kotlinx.coroutines.delay(500)
            onSearch(searchQuery)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("책 정보") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 책 표지
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bookClub?.currentBookCover?.isNotEmpty() == true) {
                            AsyncImage(
                                model = bookClub.currentBookCover,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isEditMode) {
                    // 편집 모드 - 책 검색
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("책 제목 또는 저자 검색") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )
                    
                    if (searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(searchResults.size) { index ->
                                val book = searchResults[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onBookSelected(book)
                                            isEditMode = false
                                            searchQuery = ""
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = book.cover,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = book.author,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 보기 모드 - 책 정보 표시
                    Text(
                        text = "제목",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = bookClub?.currentBook ?: "책 없음",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "지은이",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = bookClub?.currentBookAuthor ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (bookClub?.currentBookGenre?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "장르",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = bookClub.currentBookGenre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (bookClub?.currentBookDescription?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "설명",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = bookClub.currentBookDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 5
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (!isEditMode && canChangeBook) {
                    TextButton(onClick = { isEditMode = true }) {
                        Text("책 변경하기")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
        }
    )
}

@Composable
fun BookCalendarDialog(
    bookClub: com.route.readers.data.model.BookClub?,
    messages: List<ChatMessage>,
    canEditSchedule: Boolean = false,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
    onSaveBookHistory: (List<String>) -> Unit,
    onDeleteBookHistory: (List<String>) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember { 
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator 
    }
    
    val koreaTimeZone = remember { TimeZone.getTimeZone("Asia/Seoul") }
    val calendar = remember { Calendar.getInstance(koreaTimeZone) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    
    // 0: 날짜 선택 모드, 1: 일정표 모드
    var selectedTab by remember { mutableStateOf(0) }
    
    var selectedDates by remember { mutableStateOf(setOf<String>()) }
    var dragStartDate by remember { mutableStateOf<String?>(null) }
    var dragEndDate by remember { mutableStateOf<String?>(null) }
    
    // 팝업 상태
    var showPastDateAlert by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "save" or "delete"
    
    val dayFormat = remember {
        SimpleDateFormat("yyyyMMdd", Locale.KOREA).apply {
            timeZone = koreaTimeZone
        }
    }
    
    // 오늘 날짜
    val todayStr = remember {
        dayFormat.format(Calendar.getInstance(koreaTimeZone).time)
    }
    
    val messageDates = remember(messages) {
        messages.map { dayFormat.format(Date(it.timestamp)) }.toSet()
    }
    
    // 드래그 범위 계산
    fun getDateRange(start: String, end: String): Set<String> {
        val startNum = start.toIntOrNull() ?: return emptySet()
        val endNum = end.toIntOrNull() ?: return emptySet()
        val minDate = minOf(startNum, endNum)
        val maxDate = maxOf(startNum, endNum)
        return (minDate..maxDate).map { it.toString() }.toSet()
    }
    
    // 과거 날짜 포함 여부 확인
    fun containsPastDate(dates: Set<String>): Boolean {
        val today = todayStr.toIntOrNull() ?: return false
        return dates.any { (it.toIntOrNull() ?: 0) < today }
    }
    
    // 과거 날짜 알림
    if (showPastDateAlert) {
        AlertDialog(
            onDismissRequest = { showPastDateAlert = false },
            title = { Text("알림") },
            text = { Text("지난 날짜의 기록은 변경할 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = { showPastDateAlert = false }) {
                    Text("확인")
                }
            }
        )
    }
    
    // 변경/삭제 확인 다이얼로그
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { 
                showConfirmDialog = false
                pendingAction = null
            },
            title = { Text("확인") },
            text = { 
                Text(
                    if (pendingAction == "delete") "선택한 날짜의 책 표지를 삭제하시겠습니까?"
                    else "선택한 날짜에 책 표지를 저장하시겠습니까?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pendingAction == "delete") {
                        onDeleteBookHistory(selectedDates.toList())
                    } else {
                        onSaveBookHistory(selectedDates.toList())
                    }
                    selectedDates = emptySet()
                    showConfirmDialog = false
                    pendingAction = null
                }) {
                    Text(if (pendingAction == "delete") "삭제" else "저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConfirmDialog = false
                    pendingAction = null
                }) {
                    Text("취소")
                }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                // 탭 선택 (일정표는 방장만)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "날짜 선택",
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { selectedTab = 0 }
                    )
                    if (canEditSchedule) {
                        Text(
                            "일정표",
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable { selectedTab = 1 }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 년/월 네비게이션
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    }) {
                        Text("<", fontWeight = FontWeight.Bold)
                    }
                    Text("${currentYear}년 ${currentMonth + 1}월")
                    IconButton(onClick = {
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    }) {
                        Text(">", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedTab == 0) {
                    // 날짜 선택 모드 - 기존 캘린더
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val cal = Calendar.getInstance(koreaTimeZone).apply {
                        set(currentYear, currentMonth, 1)
                    }
                    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
                    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val weeks = ((firstDayOfWeek + daysInMonth + 6) / 7)
                    
                    for (week in 0 until weeks) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dayOfWeek in 0..6) {
                                val dayIndex = week * 7 + dayOfWeek - firstDayOfWeek + 1
                                if (dayIndex in 1..daysInMonth) {
                                    val dateStr = String.format("%04d%02d%02d", currentYear, currentMonth + 1, dayIndex)
                                    val hasMessage = messageDates.contains(dateStr)
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (hasMessage) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable {
                                                if (hasMessage) {
                                                    onDateSelected(dateStr)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayIndex.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // 일정표 모드 - 책 표지 저장용
                    Text(
                        text = "길게 누르고 드래그하여 범위 선택",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val cal = Calendar.getInstance(koreaTimeZone).apply {
                        set(currentYear, currentMonth, 1)
                    }
                    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
                    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val weeks = ((firstDayOfWeek + daysInMonth + 6) / 7)
                    
                    // 드래그 범위 계산
                    val rangeSelected = remember(dragStartDate, dragEndDate) {
                        if (dragStartDate != null && dragEndDate != null) {
                            getDateRange(dragStartDate!!, dragEndDate!!)
                        } else emptySet()
                    }
                    
                    // 날짜 계산 함수
                    fun getDayFromPosition(col: Int, row: Int): Int? {
                        val dayIndex = row * 7 + col - firstDayOfWeek + 1
                        return if (dayIndex in 1..daysInMonth) dayIndex else null
                    }
                    
                    var gridWidth by remember { mutableStateOf(0f) }
                    var gridHeight by remember { mutableStateOf(0f) }
                    val cellWidth = gridWidth / 7
                    val cellHeight = if (weeks > 0) gridHeight / weeks else 0f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                gridWidth = coordinates.size.width.toFloat()
                                gridHeight = coordinates.size.height.toFloat()
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                        if (cellWidth > 0 && cellHeight > 0) {
                                            val col = (offset.x / cellWidth).toInt().coerceIn(0, 6)
                                            val row = (offset.y / cellHeight).toInt().coerceIn(0, weeks - 1)
                                            getDayFromPosition(col, row)?.let { day ->
                                                val dateStr = String.format("%04d%02d%02d", currentYear, currentMonth + 1, day)
                                                dragStartDate = dateStr
                                                dragEndDate = dateStr
                                                selectedDates = emptySet()
                                            }
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (cellWidth > 0 && cellHeight > 0 && dragStartDate != null) {
                                            val col = (change.position.x / cellWidth).toInt().coerceIn(0, 6)
                                            val row = (change.position.y / cellHeight).toInt().coerceIn(0, weeks - 1)
                                            getDayFromPosition(col, row)?.let { day ->
                                                val dateStr = String.format("%04d%02d%02d", currentYear, currentMonth + 1, day)
                                                dragEndDate = dateStr
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (dragStartDate != null && dragEndDate != null) {
                                            selectedDates = getDateRange(dragStartDate!!, dragEndDate!!)
                                        }
                                        dragStartDate = null
                                        dragEndDate = null
                                    },
                                    onDragCancel = {
                                        dragStartDate = null
                                        dragEndDate = null
                                    }
                                )
                            }
                    ) {
                        Column {
                            for (week in 0 until weeks) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (dayOfWeek in 0..6) {
                                        val dayIndex = week * 7 + dayOfWeek - firstDayOfWeek + 1
                                        if (dayIndex in 1..daysInMonth) {
                                            val dateStr = String.format("%04d%02d%02d", currentYear, currentMonth + 1, dayIndex)
                                            val bookCover = bookClub?.bookHistory?.get(dateStr)
                                            val isSelected = selectedDates.contains(dateStr) || rangeSelected.contains(dateStr)
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                            bookCover?.isNotEmpty() == true -> MaterialTheme.colorScheme.surfaceVariant
                                                            else -> MaterialTheme.colorScheme.surface
                                                        }
                                                    )
                                                    .clickable {
                                                        selectedDates = if (selectedDates.contains(dateStr)) {
                                                            selectedDates - dateStr
                                                        } else {
                                                            selectedDates + dateStr
                                                        }
                                                    },
                                                contentAlignment = Alignment.TopCenter
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Text(
                                                        text = dayIndex.toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                    if (bookCover?.isNotEmpty() == true) {
                                                        AsyncImage(
                                                            model = bookCover,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .weight(1f)
                                                                .padding(2.dp)
                                                                .clip(RoundedCornerShape(2.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (selectedTab == 1 && (selectedDates.isNotEmpty() || (dragStartDate != null && dragEndDate != null))) {
                    val datesToProcess = if (dragStartDate != null && dragEndDate != null) {
                        getDateRange(dragStartDate!!, dragEndDate!!)
                    } else {
                        selectedDates
                    }
                    
                    // 삭제 버튼 (기존 기록이 있는 날짜가 선택된 경우)
                    val hasExistingRecords = datesToProcess.any { 
                        bookClub?.bookHistory?.containsKey(it) == true 
                    }
                    if (hasExistingRecords) {
                        TextButton(onClick = {
                            if (containsPastDate(datesToProcess)) {
                                showPastDateAlert = true
                            } else {
                                selectedDates = datesToProcess
                                pendingAction = "delete"
                                showConfirmDialog = true
                            }
                            dragStartDate = null
                            dragEndDate = null
                        }) {
                            Text("삭제")
                        }
                    }
                    
                    // 저장 버튼
                    TextButton(onClick = {
                        if (containsPastDate(datesToProcess)) {
                            showPastDateAlert = true
                        } else {
                            selectedDates = datesToProcess
                            pendingAction = "save"
                            showConfirmDialog = true
                        }
                        dragStartDate = null
                        dragEndDate = null
                    }) {
                        Text("저장")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
        }
    )
}


@Composable
fun MemberListDialog(
    bookClub: BookClub?,
    memberProfiles: Map<String, User>,
    currentUserRole: BookClubRole?,
    onDismiss: () -> Unit,
    onProfileClick: (String) -> Unit,
    onKickMember: (String) -> Unit,
    onSetViceOwner: (String, Boolean) -> Unit,
    getMemberRole: (String) -> BookClubRole?
) {
    val isOwner = currentUserRole == BookClubRole.OWNER
    val canKick = currentUserRole == BookClubRole.OWNER || currentUserRole == BookClubRole.VICE_OWNER
    
    var kickTargetId by remember { mutableStateOf<String?>(null) }
    
    // 강퇴 확인 다이얼로그
    kickTargetId?.let { targetId ->
        val targetName = memberProfiles[targetId]?.nickname ?: "멤버"
        AlertDialog(
            onDismissRequest = { kickTargetId = null },
            title = { Text("멤버 강퇴") },
            text = { Text("${targetName}님을 강퇴하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onKickMember(targetId)
                        kickTargetId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("강퇴") }
            },
            dismissButton = {
                TextButton(onClick = { kickTargetId = null }) { Text("취소") }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("멤버 목록 (${(bookClub?.members?.size ?: 0) + 1 + (bookClub?.viceOwners?.size ?: 0)}명)") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bookClub?.let { club ->
                    // 방장
                    item {
                        MemberItem(
                            user = memberProfiles[club.createdBy],
                            role = BookClubRole.OWNER,
                            isOwner = isOwner,
                            canKick = false,
                            onProfileClick = { onProfileClick(club.createdBy) },
                            onKickClick = {},
                            onViceOwnerToggle = {}
                        )
                    }
                    // 부방장들
                    items(club.viceOwners.size) { index ->
                        val memberId = club.viceOwners[index]
                        MemberItem(
                            user = memberProfiles[memberId],
                            role = BookClubRole.VICE_OWNER,
                            isOwner = isOwner,
                            canKick = isOwner,
                            onProfileClick = { onProfileClick(memberId) },
                            onKickClick = { kickTargetId = memberId },
                            onViceOwnerToggle = { onSetViceOwner(memberId, false) }
                        )
                    }
                    // 일반 멤버들
                    items(club.members.size) { index ->
                        val memberId = club.members[index]
                        if (!club.viceOwners.contains(memberId) && memberId != club.createdBy) {
                            MemberItem(
                                user = memberProfiles[memberId],
                                role = BookClubRole.MEMBER,
                                isOwner = isOwner,
                                canKick = canKick,
                                onProfileClick = { onProfileClick(memberId) },
                                onKickClick = { kickTargetId = memberId },
                                onViceOwnerToggle = { onSetViceOwner(memberId, true) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

@Composable
private fun MemberItem(
    user: User?,
    role: BookClubRole,
    isOwner: Boolean,
    canKick: Boolean,
    onProfileClick: () -> Unit,
    onKickClick: () -> Unit,
    onViceOwnerToggle: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (user?.profileImageUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.nickname ?: "알 수 없음",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (role) {
                    BookClubRole.OWNER -> "방장"
                    BookClubRole.VICE_OWNER -> "부방장"
                    BookClubRole.MEMBER -> "멤버"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (role) {
                    BookClubRole.OWNER -> MaterialTheme.colorScheme.primary
                    BookClubRole.VICE_OWNER -> MaterialTheme.colorScheme.tertiary
                    BookClubRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        // 방장만 관리 메뉴 표시 (자기 자신 제외)
        if ((isOwner && role != BookClubRole.OWNER) || (canKick && role == BookClubRole.MEMBER)) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Menu, contentDescription = "관리", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (isOwner) {
                        if (role == BookClubRole.MEMBER) {
                            DropdownMenuItem(
                                text = { Text("부방장 임명") },
                                onClick = {
                                    onViceOwnerToggle()
                                    showMenu = false
                                }
                            )
                        } else if (role == BookClubRole.VICE_OWNER) {
                            DropdownMenuItem(
                                text = { Text("부방장 해제") },
                                onClick = {
                                    onViceOwnerToggle()
                                    showMenu = false
                                }
                            )
                        }
                    }
                    if (canKick) {
                        DropdownMenuItem(
                            text = { Text("강퇴", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onKickClick()
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SideSheetMemberItem(
    user: User?,
    role: BookClubRole,
    isOwner: Boolean = false,
    canManage: Boolean = false,
    onClick: () -> Unit,
    onSetViceOwner: () -> Unit = {},
    onRemoveViceOwner: () -> Unit = {},
    onKick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { if (canManage && role != BookClubRole.OWNER) showMenu = true }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (user?.profileImageUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.nickname ?: "알 수 없음",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // 역할 뱃지
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = when (role) {
                BookClubRole.OWNER -> MaterialTheme.colorScheme.primary
                BookClubRole.VICE_OWNER -> MaterialTheme.colorScheme.tertiary
                BookClubRole.MEMBER -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = when (role) {
                    BookClubRole.OWNER -> "방장"
                    BookClubRole.VICE_OWNER -> "부방장"
                    BookClubRole.MEMBER -> "멤버"
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = when (role) {
                    BookClubRole.OWNER -> MaterialTheme.colorScheme.onPrimary
                    BookClubRole.VICE_OWNER -> MaterialTheme.colorScheme.onTertiary
                    BookClubRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        // 관리 메뉴
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (isOwner && role == BookClubRole.MEMBER) {
                DropdownMenuItem(
                    text = { Text("부방장 임명") },
                    onClick = {
                        showMenu = false
                        onSetViceOwner()
                    }
                )
            }
            if (isOwner && role == BookClubRole.VICE_OWNER) {
                DropdownMenuItem(
                    text = { Text("부방장 해제") },
                    onClick = {
                        showMenu = false
                        onRemoveViceOwner()
                    }
                )
            }
            if (canManage && role != BookClubRole.OWNER && !(role == BookClubRole.VICE_OWNER && !isOwner)) {
                DropdownMenuItem(
                    text = { Text("강퇴", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onKick()
                    }
                )
            }
        }
    }
}
