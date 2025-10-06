package com.route.readers.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.route.readers.data.model.Book
import com.route.readers.data.model.User
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToFollowList: (listType: String, nickname: String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    LaunchedEffect(key1 = userId) {
        viewModel.fetchUserProfile(userId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필") },
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
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> CircularProgressIndicator()
                is ProfileUiState.Error -> Text(text = state.message)
                is ProfileUiState.Success -> {
                    ProfileContent(
                        user = state.user,
                        isMyProfile = state.isMyProfile,
                        isFollowing = state.isFollowing,
                        recommendedBooks = state.recommendedBooks,
                        favoriteBooks = state.favoriteBooks,
                        onFollowClick = { viewModel.followUser(state.user.uid) },
                        onUnfollowClick = { viewModel.unfollowUser(state.user.uid) },
                        onFollowListClick = { listType ->
                            onNavigateToFollowList(listType, state.user.nickname)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileContent(
    user: User,
    isMyProfile: Boolean,
    isFollowing: Boolean,
    recommendedBooks: List<Book>,
    favoriteBooks: List<Book>,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onFollowListClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ProfileInfoSection(
                user = user,
                isMyProfile = isMyProfile,
                isFollowing = isFollowing,
                onFollowClick = onFollowClick,
                onUnfollowClick = onUnfollowClick,
                onFollowListClick = onFollowListClick
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
                        user.readingGenres.forEach { genre ->
                            Chip(label = genre)
                        }
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
                        user.readingStyles.forEach { style ->
                            Chip(label = style)
                        }
                    }
                }
            }
        }

        if (recommendedBooks.isNotEmpty()) {
            item {
                RecommendedBooksSection(
                    books = recommendedBooks,
                    onBookClick = { }
                )
            }
        }

        if (favoriteBooks.isNotEmpty()) {
            item {
                FavoriteBooksSection(
                    books = favoriteBooks,
                    onBookClick = { }
                )
            }
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
    onFollowListClick: (String) -> Unit
) {
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
            ProfileImage(imageUrl = user.profileImageUrl, nickname = user.nickname)
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
                BookCardItem(book = book, onClick = { onBookClick(book) })
            }
        }
    }
}

@Composable
fun FavoriteBooksSection(
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "관심 도서",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(books, key = { it.isbn }) { book ->
                BookCardItem(book = book, onClick = { onBookClick(book) })
            }
        }
    }
}

@Composable
fun BookCardItem(book: Book, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        Card(shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
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
fun ProfileImage(imageUrl: String?, nickname: String) {
    Box(
        modifier = Modifier
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

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val fakeUser = User(
        uid = "previewUser",
        nickname = "책벌레독서가",
        level = 8,
        followerCount = 234,
        followingCount = 89,
        readBookCount = 47,
        readingGenres = listOf("소설", "자기계발", "역사", "SF", "에세이", "고전"),
        readingStyles = listOf("한 분야 깊게 파기", "천천히 음미하기", "다양하게 맛보기")
    )
    val fakeBooks = listOf(
        Book(title = "불편한 편의점", author = "김호연", cover = ""),
        Book(title = "세이노의 가르침", author = "세이노", cover = ""),
        Book(title = "역행자", author = "자청", cover = "")
    )
    val favoriteBooks = listOf(
        Book(title = "코스모스", author = "칼 세이건", cover = ""),
        Book(title = "사피엔스", author = "유발 하라리", cover = "")
    )

    MaterialTheme {
        ProfileContent(
            user = fakeUser,
            isMyProfile = false,
            isFollowing = true,
            recommendedBooks = fakeBooks,
            favoriteBooks = favoriteBooks,
            onFollowClick = {},
            onUnfollowClick = {},
            onFollowListClick = {}
        )
    }
}
