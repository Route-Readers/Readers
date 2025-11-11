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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import androidx.compose.material3.MaterialTheme
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
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.User
import com.route.readers.ui.screens.feed.FeedCard
import com.route.readers.ui.screens.feed.FeedItem
import com.route.readers.ui.theme.DarkRed
import kotlinx.coroutines.launch
import kotlin.text.isNotEmpty
import kotlin.text.toFloat

@Composable
fun ProfileScreen(
    userId: String,
    onNavigateToFollowList: (listType: String, nickname: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMyBookList: () -> Unit,
    onNavigateToLevel: () -> Unit,
    onNavigateToCustomization: () -> Unit = {},
    onNavigateToGoal: () -> Unit,
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
        containerColor = MaterialTheme.colorScheme.background
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
                    val levelInfo = calculateLevelInfo(state.user.totalPoints, state.user.level)
                    ProfileContent(
                        state = state,
                        level = levelInfo.currentLevel,
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
                        onNavigateToGoal = onNavigateToGoal,
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
    level: Int,
    viewModel: ProfileViewModel,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onFollowListClick: (String) -> Unit,
    onUpdateProfileImage: (Uri) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToMyBookList: () -> Unit,
    onNavigateToLevel: () -> Unit,
    onNavigateToGoal: () -> Unit,
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
                TextButton(onClick = { showDeleteDialog = false }) { Text("아니요", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            ProfileInfoSection(
                user = user,
                level = level,
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

        if (state.isMyProfile) {
            item {
                GoalSettingSection(onSetGoalClick = onNavigateToGoal)
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
                    Text("차단된 사용자입니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    "taste" -> Icons.Rounded.AutoStories
                                    "recommended" -> Icons.Rounded.LocalFireDepartment
                                    "favorite" -> Icons.Rounded.FavoriteBorder
                                    "challenges" -> Icons.Rounded.EmojiEvents
                                    "achievements" -> Icons.Rounded.WorkspacePremium
                                    "posts" -> Icons.Rounded.GridView
                                    else -> Icons.Rounded.GridView
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
                                                Text("선호 장르", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(user.readingGenres) { genre -> Chip(label = genre) }
                                                }
                                            }
                                            if (user.readingStyles.isNotEmpty()) {
                                                Text("독서 스타일", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        ongoingChallenges = emptyList(),
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
                                        onDeleteClick = { feedId: String ->
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
                                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("독서", "출석")

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
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

        TabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = DarkRed,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                    color = DarkRed
                )
            }
        ) {
            categories.forEachIndexed { index, title ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = { Text(text = title) }
                )
            }
        }

        val achievementsToShow = (if (selectedTabIndex == 0) ongoingAchievements else completedAchievements)
            .filter { it.category == categories[selectedCategoryIndex] }

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                achievementsToShow.groupBy { it.category }.forEach { (category, achievements) ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    achievements.forEach { achievement ->
                        AchievementItem(achievement = achievement, onClick = { onAchievementClick(achievement) })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}


@Composable
fun AchievementItem(achievement: Achievement, onClick: () -> Unit) {
    val progress = (achievement.currentProgress.toFloat() / achievement.targetProgress.toFloat()).coerceIn(0f, 1f)
    val isCompleted = achievement.isCompleted

    val cardBackgroundColor = if (isCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
    val cardBorderColor = if (isCompleted) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val progressColor = if (isCompleted) Color(0xFF4CAF50) else DarkRed

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
                Text(text = achievement.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
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
            Text(text = achievement.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${achievement.currentProgress} / ${achievement.targetProgress}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun ProfileInfoSection(
    user: User,
    level: Int,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            Text(text = user.nickname, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelChip(level = level, onClick = onNavigateToLevel)
                user.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ReadingStreakSection(
                attendanceDays = user.consecutiveDays,
                readingDays = user.consecutiveReadingDays
            )

            Spacer(modifier = Modifier.height(8.dp))
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
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.secondary else DarkRed,
                                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = if (isFollowing) "언팔로우" else "팔로우")
                        }
                        OutlinedButton(
                            onClick = onBlockUser,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(text = "차단하기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalSettingSection(onSetGoalClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onSetGoalClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkRed.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "나만의 독서 목표를 설정해보세요!",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "목표를 설정하고 꾸준한 독서 습관을 만들어보세요.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "목표 설정으로 이동",
                tint = DarkRed
            )
        }
    }
}

@Composable
fun ReadingStreakSection(attendanceDays: Int, readingDays: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreakItem(
                icon = Icons.Rounded.LocalFireDepartment,
                label = "연속 출석",
                days = attendanceDays,
                iconColor = Color(0xFF81C784)
            )
            StreakItem(
                icon = Icons.Rounded.LocalFireDepartment,
                label = "연속 독서",
                days = readingDays,
                iconColor = Color(0xFFE57373)
            )
        }
    }
}

@Composable
fun StreakItem(icon: ImageVector, label: String, days: Int, iconColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "$days 일",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
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
                    Icon(Icons.Default.Delete, contentDescription = "선택한 도서 삭제", tint = MaterialTheme.colorScheme.onSurface)
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = book.author,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    "cat" -> R.drawable.cat
                    "dog" -> R.drawable.dog
                    "fox" -> R.drawable.fox
                    "rabbit" -> R.drawable.rabbit
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
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .background(color = DarkRed.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
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
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
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
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "관심 도서를 등록하러 가기",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (challenges.isEmpty()) {
            Text(
                "아직 ${title}가 없어요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
                Text(text = challenge.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                if (challenge.completed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "완료됨",
                        tint = Color(0xFF27AE60)
                    )
                } else {
                    Text(text = "${challenge.progress}%", color = DarkRed, fontWeight = FontWeight.Bold)
                }
            }
            Text(text = challenge.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LinearProgressIndicator(
                progress = { challenge.progress.toString().toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = DarkRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkRed.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = DarkRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = if (isExpanded) "접기" else "펼치기",
                modifier = Modifier.rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("비공개 계정입니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        Text("콘텐츠를 보려면 이 계정을 팔로우하세요.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun LevelChip(level: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}