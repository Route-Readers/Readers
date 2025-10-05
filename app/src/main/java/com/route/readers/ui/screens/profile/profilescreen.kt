package com.route.readers.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.User
import com.route.readers.data.model.ProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onFollowersClick: (String) -> Unit,
    onFollowingClick: (String) -> Unit,
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
                                        FirebaseAuth.getInstance().signOut()
                                        onNavigateToLogin()
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
                        onFollowClick = { viewModel.followUser(state.user.uid) },
                        onUnfollowClick = { viewModel.unfollowUser(state.user.uid) },
                        onFollowersClick = { onFollowersClick(state.user.uid) },
                        onFollowingClick = { onFollowingClick(state.user.uid) }
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
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    val primaryRed = Color(0xFFC0392B)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                ProfileImage(
                    imageUrl = user.profileImageUrl,
                    nickname = user.nickname,
                    size = 100.dp,
                    backgroundColor = primaryRed
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = user.nickname, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                Chip(label = "레벨 ${user.level}", backgroundColor = Color(0xFFF5E1DF), contentColor = primaryRed)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileInfoItem(count = user.followerCount.toString(), label = "팔로워", onClick = onFollowersClick)
                    ProfileInfoItem(count = user.followingCount.toString(), label = "팔로잉", onClick = onFollowingClick)
                    ProfileInfoItem(count = user.readBookCount.toString(), label = "읽은 책")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isMyProfile) {
                    Button(
                        onClick = { if (isFollowing) onUnfollowClick() else onFollowClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color.Gray else primaryRed
                        )
                    ) {
                        Text(text = if (isFollowing) "언팔로우" else "팔로우")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (user.readingGenres.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("선호 장르", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        user.readingGenres.forEach { genre ->
                            Chip(label = genre, backgroundColor = Color(0xFFF5E1DF), contentColor = primaryRed)
                        }
                    }
                }
            }
        }

        // ✨✨✨ 새로 추가된 독서 스타일 카드 ✨✨✨
        if (user.readingStyles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("독서 스타일", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        user.readingStyles.forEach { style ->
                            Chip(label = style, backgroundColor = Color(0xFFF5E1DF), contentColor = primaryRed)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ProfileImage(
    imageUrl: String?,
    nickname: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 100.dp,
    backgroundColor: Color
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = "프로필 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = nickname.firstOrNull()?.toString() ?: "",
                color = Color.White,
                fontSize = (size.value / 2.5).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun ProfileInfoItem(count: String, label: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Text(text = count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC0392B))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun Chip(label: String, backgroundColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = contentColor, fontSize = 12.sp)
    }
}

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
        readingGenres = listOf("소설", "자기계발", "역사", "SF"),
        // Preview에도 readingStyles 추가
        readingStyles = listOf("한 분야 깊게 파기", "천천히 음미하기")
    )

    MaterialTheme {
        ProfileContent(
            user = fakeUser,
            isMyProfile = false,
            isFollowing = true,
            onFollowClick = {},
            onUnfollowClick = {},
            onFollowersClick = {},
            onFollowingClick = {}
        )
    }
}
