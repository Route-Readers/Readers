package com.route.readers.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.route.readers.data.model.Book
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.User
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToFollowList: (listType: String, nickname: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: ProfileViewModel
) {
    LaunchedEffect(key1 = userId) {
        viewModel.fetchUserProfile(userId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val isSelectionModeActive = (uiState as? ProfileUiState.Success)?.isSelectionMode == true
    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelectionMode()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                val nickname = (uiState as? ProfileUiState.Success)?.user?.nickname
                Text(text = nickname ?: "프로필")
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                }
            },
            actions = {
                if ((uiState as? ProfileUiState.Success)?.isMyProfile == true) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "설정")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("로그아웃") },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color.Black
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> CircularProgressIndicator()
                is ProfileUiState.Error -> Text(text = state.message)
                is ProfileUiState.Success -> {
                    ProfileContent(
                        state = state,
                        onFollowClick = { viewModel.followUser(state.user.uid) },
                        onUnfollowClick = { viewModel.unfollowUser(state.user.uid) },
                        onFollowListClick = { listType ->
                            onNavigateToFollowList(listType, state.user.nickname)
                        },
                        onUpdateProfileImage = { imageUri ->
                            viewModel.updateProfileImage(imageUri)
                        },
                        onNavigateToSearch = onNavigateToSearch,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileContent(
    state: ProfileUiState.Success,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onFollowListClick: (String) -> Unit,
    onUpdateProfileImage: (android.net.Uri) -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: ProfileViewModel
) {
    val user = state.user
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ProfileInfoSection(
                user = user,
                isMyProfile = state.isMyProfile,
                isFollowing = state.isFollowing,
                onFollowClick = onFollowClick,
                onUnfollowClick = onUnfollowClick,
                onFollowListClick = onFollowListClick,
                onUpdateProfileImage = onUpdateProfileImage
            )
        }

        if (user.readingGenres.isNotEmpty()) {
            item {
                ProfileDetailCard("선호 장르") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        user.readingGenres.forEach { genre -> Chip(label = genre) }
                    }
                }
            }
        }

        if (user.readingStyles.isNotEmpty()) {
            item {
                ProfileDetailCard("독서 스타일") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        user.readingStyles.forEach { style -> Chip(label = style) }
                    }
                }
            }
        }

        if (state.recommendedBooks.isNotEmpty()) {
            item {
                RecommendedBooksSection(
                    books = state.recommendedBooks,
                    onBookClick = { /* 도서 상세 화면으로 이동 */ }
                )
            }
        }

        item {
            FavoriteBooksSection(
                books = state.favoriteBooks,
                isSelectionMode = state.isSelectionMode,
                selectedBookIds = state.selectedBookIds,
                onToggleSelection = viewModel::toggleBookSelection,
                onStartSelectionMode = viewModel::startSelectionMode,
                onDeleteClick = viewModel::deleteSelectedFavoriteBooks,
                onBookClick = { /* 도서 상세 화면으로 이동 */ },
                onNavigateToSearch = onNavigateToSearch
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            ChallengesSection(
                ongoingChallenges = state.ongoingChallenges,
                completedChallenges = state.completedChallenges,
                onChallengeClick = { /* 챌린지 상세 화면으로 이동 */ }
            )
        }
    }
}

@Composable
fun ProfileInfoSection(
    user: User,
    isMyProfile: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onFollowListClick: (String) -> Unit,
    onUpdateProfileImage: (android.net.Uri) -> Unit
) {

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { onUpdateProfileImage(it) }
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileImage(
                imageUrl = user.profileImageUrl,
                nickname = user.nickname,
                isMyProfile = isMyProfile,
                onImageClick = {
                    singlePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = user.nickname, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Chip(label = "레벨 ${user.level}")
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileInfoItem(count = user.followerCount.toString(), label = "팔로워", onClick = { onFollowListClick("followers") })
                ProfileInfoItem(count = user.followingCount.toString(), label = "팔로잉", onClick = { onFollowListClick("following") })
                ProfileInfoItem(count = user.readBookCount.toString(), label = "읽은 책")
            }
            if (!isMyProfile) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { if (isFollowing) onUnfollowClick() else onFollowClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color.Gray else DarkRed
                    )
                ) {
                    Text(text = if (isFollowing) "언팔로우" else "팔로우")
                }
            }
        }
    }
}

@Composable
fun ProfileDetailCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun RecommendedBooksSection(
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "회원님을 위한 추천 도서",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(books, key = { it.isbn }) { book ->
                BookCardItem(
                    book = book,
                    onClick = { onBookClick(book) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteBooksSection(
    books: List<Book>,
    isSelectionMode: Boolean,
    selectedBookIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onStartSelectionMode: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onBookClick: (Book) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "관심 도서",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (isSelectionMode) {
                IconButton(onClick = { if (selectedBookIds.isNotEmpty()) showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "선택한 도서 삭제")
                }
            }
        }

        if (books.isEmpty()) {
            EmptyFavoriteBooks(onNavigateToSearch = onNavigateToSearch)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(books, key = { it.isbn }) { book ->
                    BookCardItem(
                        book = book,
                        isSelected = book.isbn in selectedBookIds,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(book.isbn)
                                } else {
                                    onBookClick(book)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    onStartSelectionMode(book.isbn)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("관심 도서 삭제") },
            text = { Text("선택한 ${selectedBookIds.size}개의 도서를 관심 도서에서 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun BookCardItem(
    book: Book,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val finalModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Column(
        modifier = finalModifier.width(120.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Card(shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(120.dp)
                        .height(170.dp)
                        .background(Color.LightGray)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "선택됨",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = book.author,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProfileImage(
    imageUrl: String?,
    nickname: String,
    isMyProfile: Boolean,
    onImageClick: () -> Unit
) {
    val modifier = if (isMyProfile) {
        Modifier.clickable(onClick = onImageClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(DarkRed),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "프로필 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = nickname.firstOrNull()?.toString() ?: "",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (isMyProfile) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "프로필 사진 변경",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileInfoItem(count: String, label: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(4.dp)) {
        Text(text = count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkRed)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun Chip(label: String) {
    Box(
        modifier = Modifier
            .background(color = Color(0xFFF5E1DF), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = DarkRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmptyFavoriteBooks(onNavigateToSearch: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = Color.LightGray.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onNavigateToSearch() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "관심 도서 추가",
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF5F5F5), CircleShape)
                    .padding(8.dp),
                tint = Color.Gray
            )
            Text(
                text = "관심 도서를 등록하러 가기",
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ChallengesSection(
    ongoingChallenges: List<Challenge>,
    completedChallenges: List<Challenge>,
    onChallengeClick: (Challenge) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ChallengeCategory(
            title = "진행 중인 챌린지",
            challenges = ongoingChallenges,
            onChallengeClick = onChallengeClick
        )
        ChallengeCategory(
            title = "완료한 챌린지",
            challenges = completedChallenges,
            onChallengeClick = onChallengeClick
        )
    }
}

@Composable
fun ChallengeCategory(
    title: String,
    challenges: List<Challenge>,
    onChallengeClick: (Challenge) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
        )
        if (challenges.isEmpty()) {
            Text(
                "아직 ${title.replace(" ", "이 ")} 없어요.",
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            challenges.forEach { challenge ->
                ChallengeItem(challenge = challenge, onClick = { onChallengeClick(challenge) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ChallengeItem(challenge: Challenge, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = challenge.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (challenge.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "완료됨",
                        tint = Color(0xFF27AE60)
                    )
                } else {
                    Text(text = "${challenge.progress}%", color = DarkRed, fontWeight = FontWeight.Bold)
                }
            }
            Text(text = challenge.description, fontSize = 14.sp, color = Color.Gray)
            LinearProgressIndicator(
                progress = { challenge.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = DarkRed,
                trackColor = Color.LightGray.copy(alpha = 0.5f)
            )
        }
    }
}
