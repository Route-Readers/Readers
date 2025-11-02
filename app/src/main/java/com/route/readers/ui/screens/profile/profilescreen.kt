package com.route.readers.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.route.readers.R
import com.route.readers.data.model.Book
import com.route.readers.data.model.Challenge // Challenge 모델은 여전히 필요할 수 있으므로 유지
import com.route.readers.data.model.User
import com.route.readers.ui.screens.feed.FeedCard
import com.route.readers.ui.screens.feed.FeedItem
import com.route.readers.ui.theme.DarkRed
import kotlinx.coroutines.launch
import kotlin.text.toFloat

// 불필요한 import문이 있다면 제거해도 좋습니다.

@Composable
fun ProfileScreen(
    userId: String,
    onNavigateToFollowList: (listType: String, nickname: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMyBookList: () -> Unit,
    onNavigateToLevel: () -> Unit,
    onNavigateToCustomization: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    var showCustomization by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(key1 = userId) {
        viewModel.fetchUserProfile(userId)
        viewModel.syncFollowRelationship(userId)
    }

    val uiState by viewModel.uiState.collectAsState()

    val isSelectionModeActive = (uiState as? ProfileUiState.Success)?.isSelectionMode == true
    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelectionMode()
    }

    val currentState = uiState
    if (showCustomization && currentState is ProfileUiState.Success) {
        ProfileCustomizationScreen(
            currentCharacter = currentState.user.profileCharacter,
            currentBackgroundColor = currentState.user.profileBackgroundColor,
            nickname = currentState.user.nickname,
            onSave = { character, backgroundColor ->
                viewModel.updateProfileCharacter(character, backgroundColor)
                showCustomization = false
            },
            onBack = { showCustomization = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> CircularProgressIndicator()
                is ProfileUiState.Error -> Text(text = state.message)
                is ProfileUiState.Success -> {
                    ProfileContent(
                        state = state,
                        viewModel = viewModel,
                        onFollowClick = {
                            viewModel.followUser(state.user.uid)
                            viewModel.syncFollowRelationship(state.user.uid)
                        },
                        onUnfollowClick = {
                            viewModel.unfollowUser(state.user.uid)
                            viewModel.syncFollowRelationship(state.user.uid)
                        },
                        onFollowListClick = { listType ->
                            onNavigateToFollowList(listType, state.user.nickname)
                        },
                        onUpdateProfileImage = { imageUri ->
                            if (imageUri == Uri.EMPTY) {
                                showCustomization = true
                            } else {
                                viewModel.updateProfileImage(imageUri)
                            }
                        },
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToCustomization = {
                            showCustomization = true
                        },
                        onNavigateToMyBookList = onNavigateToMyBookList,
                        onNavigateToLevel = onNavigateToLevel,
                        onBlockUser = { viewModel.blockUser(state.user.uid) },
                        onUnblockUser = { viewModel.unblockUser(state.user.uid) },
                        onBookmarkClick = { feedId, isBookmarked ->
                            viewModel.toggleBookmark(feedId, isBookmarked)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isBookmarked) "북마크에서 삭제했습니다." else "북마크에 추가했습니다."
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileContent(
    state: ProfileUiState.Success,
    viewModel: ProfileViewModel,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onFollowListClick: (String) -> Unit,
    onUpdateProfileImage: (Uri) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToMyBookList: () -> Unit,
    onNavigateToLevel: () -> Unit,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit,
    onBookmarkClick: (String, Boolean) -> Unit
) {
    val user = state.user
    val isPrivateAndNotFollowing = user.isPrivate && !state.isMyProfile && !state.isFollowing

    var showDeleteDialog by remember { mutableStateOf(false) }
    var feedToDelete by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog && feedToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("피드 삭제") },
            text = { Text("피드를 정말 지우시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feedToDelete?.let { viewModel.deleteFeed(it) }
                        showDeleteDialog = false
                    }
                ) { Text("예", color = DarkRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("아니요", color = Color.Gray) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileInfoSection(
                    user = user,
                    isMyProfile = state.isMyProfile,
                    isFollowing = state.isFollowing,
                    isBlocked = state.isBlocked,
                    onFollowClick = onFollowClick,
                    onUnfollowClick = onUnfollowClick,
                    onFollowListClick = onFollowListClick,
                    onUpdateProfileImage = onUpdateProfileImage,
                    onNavigateToCustomization = onNavigateToCustomization,
                    onNavigateToMyBookList = onNavigateToMyBookList,
                    onNavigateToLevel = onNavigateToLevel,
                    onBlockUser = onBlockUser,
                    onUnblockUser = onUnblockUser
                )
            }
        }

        if (state.isBlocked) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("차단된 사용자입니다.", color = Color.Gray)
                }
            }
        } else if (isPrivateAndNotFollowing) {
            item {
                PrivateProfileContent()
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        var expandedState by remember { mutableStateOf<String?>("posts") }
                        val sections = mutableListOf<Pair<String, String>>()

                        if (user.readingGenres.isNotEmpty() || user.readingStyles.isNotEmpty()) {
                            sections.add("taste" to "독서 취향")
                        }
                        if (state.recommendedBooks.isNotEmpty()) {
                            sections.add("recommended" to "추천 도서")
                        }
                        sections.add("favorite" to "관심 도서")
                        sections.add("challenges" to "독서 챌린지")
                        sections.add("achievements" to "업적")
                        sections.add("posts" to "내 활동")

                        sections.forEachIndexed { index, (key, title) ->
                            ExpandableProfileSection(
                                title = title,
                                icon = when (key) {
                                    "taste" -> Icons.Rounded.Palette
                                    "recommended" -> Icons.Rounded.Star
                                    "favorite" -> Icons.Rounded.Favorite
                                    "challenges" -> Icons.Default.CheckCircle
                                    "achievements" -> Icons.Rounded.EmojiEvents
                                    "posts" -> Icons.Rounded.Bookmark
                                    else -> Icons.Rounded.Bookmark
                                },
                                isExpanded = expandedState == key,
                                onToggle = {
                                    expandedState = if (expandedState == key) null else key
                                }
                            ) {
                                when (key) {
                                    "taste" -> {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            if (user.readingGenres.isNotEmpty()) {
                                                Text("선호 장르", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(user.readingGenres) { genre -> Chip(label = genre) }
                                                }
                                            }
                                            if (user.readingStyles.isNotEmpty()) {
                                                Text("독서 스타일", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(user.readingStyles) { style -> Chip(label = style) }
                                                }
                                            }
                                        }
                                    }

                                    "recommended" -> RecommendedBooksSection(
                                        books = state.recommendedBooks,
                                        onBookClick = { }
                                    )

                                    "favorite" -> FavoriteBooksSection(
                                        books = state.favoriteBooks,
                                        isSelectionMode = state.isSelectionMode,
                                        selectedBookIds = state.selectedBookIds,
                                        onToggleSelection = viewModel::toggleBookSelection,
                                        onStartSelectionMode = viewModel::startSelectionMode,
                                        onDeleteClick = viewModel::deleteSelectedFavoriteBooks,
                                        onBookClick = { },
                                        onNavigateToSearch = onNavigateToSearch
                                    )

                                    "challenges" -> ChallengesSection(
                                        ongoingChallenges = emptyList(), // 하드코딩된 값 제거 후 빈 리스트 전달
                                        completedChallenges = emptyList(),
                                        onChallengeClick = { }
                                    )

                                    "achievements" -> AchievementsSection(
                                        ongoingAchievements = state.ongoingAchievements,
                                        completedAchievements = state.completedAchievements,
                                        onAchievementClick = { }
                                    )

                                    "posts" -> PostsSection(
                                        myPosts = state.myPosts.filterIsInstance<FeedItem.BookReview>()
                                            .sortedByDescending { it.timestamp },
                                        savedPosts = state.savedPosts,
                                        isMyProfile = state.isMyProfile,
                                        likedFeedIds = state.likedFeedIds,
                                        bookmarkedFeedIds = state.bookmarkedFeedIds,
                                        onLikeClick = viewModel::toggleLike,
                                        onBookmarkClick = onBookmarkClick,
                                        onDeleteClick = { feedId ->
                                            feedToDelete = feedId
                                            showDeleteDialog = true
                                        },
                                        wishlist = state.wishlist,
                                        myLibrary = state.myLibrary,
                                        onToggleWishlist = viewModel::toggleWishlist,
                                        onToggleMyLibrary = viewModel::toggleMyLibrary,
                                        userInfoMap = state.userInfoMap
                                    )
                                }
                            }
                            if (index < sections.size - 1) {
                                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AchievementsSection(
    ongoingAchievements: List<Achievement>,
    completedAchievements: List<Achievement>,
    onAchievementClick: (Achievement) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("진행 중", "완료")

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = DarkRed,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = DarkRed
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title) }
                )
            }
        }

        val achievementsToShow = if (selectedTabIndex == 0) ongoingAchievements else completedAchievements

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (achievementsToShow.isEmpty()) {
                Text(
                    text = if (selectedTabIndex == 0) "진행 중인 업적이 없어요." else "완료한 업적이 없어요.",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                achievementsToShow.forEach { achievement ->
                    AchievementItem(achievement = achievement, onClick = { onAchievementClick(achievement) })
                }
            }
        }
    }
}


@Composable
fun AchievementItem(achievement: Achievement, onClick: () -> Unit) {
    val progress = (achievement.currentProgress.toFloat() / achievement.targetProgress.toFloat()).coerceIn(0f, 1f)
    val isCompleted = achievement.isCompleted

    val cardBackgroundColor = if (isCompleted) Color(0xFFE8F5E9) else Color.White
    val cardBorderColor = if (isCompleted) Color(0xFFC8E6C9) else Color.LightGray.copy(alpha = 0.5f)
    val progressColor = if (isCompleted) Color(0xFF4CAF50) else Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = achievement.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("완료", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            Text(text = achievement.description, fontSize = 14.sp, color = Color.DarkGray)

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${achievement.currentProgress} / ${achievement.targetProgress}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = progressColor,
                trackColor = Color.LightGray.copy(alpha = 0.4f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsSection(
    myPosts: List<FeedItem>,
    savedPosts: List<FeedItem>,
    isMyProfile: Boolean,
    likedFeedIds: Set<String>,
    bookmarkedFeedIds: Set<String>,
    onLikeClick: (feedId: String, isLiked: Boolean) -> Unit,
    onBookmarkClick: (feedId: String, isBookmarked: Boolean) -> Unit,
    onDeleteClick: (feedId: String) -> Unit,
    wishlist: List<String>,
    myLibrary: List<String>,
    onToggleWishlist: (Book, Boolean) -> Unit,
    onToggleMyLibrary: (Book, Boolean) -> Unit,
    userInfoMap: Map<String, User>
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = if (isMyProfile) listOf("내가 쓴 글", "저장한 글") else listOf("작성한 글")

    Column(Modifier.background(Color(0xFFF5F5F5))) {
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = DarkRed,
            indicator = {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                    color = DarkRed
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title) }
                )
            }
        }

        val postsToShow = when {
            selectedTabIndex == 0 -> myPosts
            selectedTabIndex == 1 && isMyProfile -> savedPosts
            else -> emptyList()
        }

        if (postsToShow.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTabIndex == 0) "작성한 글이 없습니다." else "저장한 글이 없습니다.",
                    color = Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                postsToShow.forEach { post ->
                    val isLiked = likedFeedIds.contains(post.id)
                    val isBookmarked = bookmarkedFeedIds.contains(post.id)

                    FeedCard(
                        item = post,
                        isLiked = isLiked,
                        isBookmarked = isBookmarked,
                        onLikeClick = { onLikeClick(post.id, isLiked) },
                        onBookmarkClick = { onBookmarkClick(post.id, isBookmarked) },
                        onDeleteClick = { onDeleteClick(post.id) },
                        onUserClick = { },
                        wishlist = wishlist,
                        myLibrary = myLibrary,
                        onToggleWishlist = onToggleWishlist,
                        onToggleMyLibrary = onToggleMyLibrary,
                        followerInfoMap = userInfoMap
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoSection(
    user: User,
    isMyProfile: Boolean,
    isFollowing: Boolean,
    isBlocked: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onFollowListClick: (String) -> Unit,
    onUpdateProfileImage: (Uri) -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToMyBookList: () -> Unit,
    onNavigateToLevel: () -> Unit,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { onUpdateProfileImage(it) }
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileImage(
                user = user,
                isMyProfile = isMyProfile,
                onImageClick = {
                    onNavigateToCustomization()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = user.nickname, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Chip(label = "레벨 ${user.level}", onClick = onNavigateToLevel)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileInfoItem(count = user.followerCount.toString(), label = "팔로워", onClick = { onFollowListClick("followers") })
                ProfileInfoItem(count = user.followingCount.toString(), label = "팔로잉", onClick = { onFollowListClick("following") })
                ProfileInfoItem(
                    count = user.readBookCount.toString(),
                    label = "읽은 책",
                    onClick = if (isMyProfile) onNavigateToMyBookList else null
                )
            }

            if (!isMyProfile) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isBlocked) {
                        Button(
                            onClick = onUnblockUser,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkRed
                            )
                        ) {
                            Text(text = "차단 해제")
                        }
                    } else {
                        Button(
                            onClick = { if (isFollowing) onUnfollowClick() else onFollowClick() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) Color.Gray else DarkRed
                            )
                        ) {
                            Text(text = if (isFollowing) "언팔로우" else "팔로우")
                        }
                        OutlinedButton(
                            onClick = onBlockUser,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text(text = "차단하기", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendedBooksSection(
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
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
        if (isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
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
                contentPadding = PaddingValues(horizontal = 16.dp)
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
                    Text("삭제", color = DarkRed)
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
    user: User,
    isMyProfile: Boolean,
    onImageClick: () -> Unit
) {
    val modifier = if (isMyProfile) {
        Modifier.clickable(onClick = onImageClick)
    } else {
        Modifier
    }

    val backgroundColor = try {
        user.profileBackgroundColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: DarkRed
    } catch (e: Exception) {
        DarkRed
    }

    Box(
        modifier = modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            !user.profileImageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "프로필 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            user.profileCharacter != null -> {
                val drawableRes = when (user.profileCharacter) {
                    "lion" -> R.drawable.lion
                    "penguin" -> R.drawable.penguin
                    "redpanda" -> R.drawable.redpanda
                    "squirrel" -> R.drawable.squirrel
                    else -> null
                }

                drawableRes?.let {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .build(),
                        contentDescription = "캐릭터",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            else -> {
                Text(
                    text = user.nickname.firstOrNull()?.toString() ?: "",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
fun Chip(label: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
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
            .padding(horizontal = 16.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (challenges.isEmpty()) {
            Text(
                "아직 ${title}가 없어요.",
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                challenges.forEach { challenge ->
                    ChallengeItem(challenge = challenge, onClick = { onChallengeClick(challenge) })
                }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
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

            // ▼▼▼ 다른 AI가 알려준 최종 수정 부분 ▼▼▼
            LinearProgressIndicator(
                progress = { challenge.progress.toString().toFloat() / 100f }, // Int를 Float으로 변환하여 실수 나눗셈 수행
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = DarkRed,
                trackColor = Color.LightGray.copy(alpha = 0.5f)
            )
            // ▲▲▲ 여기까지 수정 ▲▲▲
        }
    }
}

@Composable
fun ExpandableProfileSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "rotation")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = when (icon) {
                        Icons.Rounded.Palette -> Color(0xFF9CCC65)
                        Icons.Rounded.Star -> Color(0xFFEC407A)
                        Icons.Rounded.Favorite -> Color(0xFF42A5F5)
                        Icons.Default.CheckCircle -> Color(0xFFFFCA28)
                        Icons.Rounded.EmojiEvents -> Color(0xFFFFA726)
                        else -> Color.Gray
                    }
                )
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = if (isExpanded) "접기" else "펼치기",
                modifier = Modifier.rotate(rotationAngle),
                tint = Color.Gray
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PrivateProfileContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "비공개 계정",
            modifier = Modifier.size(48.dp),
            tint = Color.Gray
        )
        Text("비공개 계정입니다.", color = Color.Gray, fontSize = 16.sp)
        Text("콘텐츠를 보려면 이 계정을 팔로우하세요.", color = Color.Gray, fontSize = 14.sp)
    }
}