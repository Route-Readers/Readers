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
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
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
import com.route.readers.data.model.ChallengeType
import com.route.readers.data.model.User
import com.route.readers.ui.components.AdBanner
import com.route.readers.ui.screens.feed.FeedCard
import com.route.readers.ui.screens.feed.FeedItem
import com.route.readers.ui.screens.feed.FeedViewModel
import com.route.readers.ui.theme.DarkRed
import kotlinx.coroutines.launch
import kotlin.text.isNotEmpty
import kotlin.text.toFloat

val Gold = Color(0xFFD4AF37)
val Burgundy = Color(0xFF800020)
val PremiumGradient = Brush.linearGradient(
    colors = listOf(Burgundy, Color(0xFF500010)),
    start = Offset(0f, 0f),
    end = Offset(1000f, 1000f)
)
val CardShadow = Shadow(
    color = Color.Black.copy(alpha = 0.25f),
    offset = Offset(0f, 4f),
    blurRadius = 8f
)

@Composable
fun ProfileScreen(
    userId: String,
    onNavigateToFollowList: (listType: String, nickname: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMyBookList: () -> Unit,
    onNavigateToLevel: () -> Unit,
    onNavigateToCustomization: () -> Unit = {},
    onNavigateToGoal: () -> Unit,
    onNavigateToOtherUserProfile: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = userId) {
        viewModel.fetchUserProfile(userId)
        viewModel.syncFollowRelationship(userId)
    }

    val uiState by viewModel.uiState.collectAsState()

    val isSelectionModeActive = (uiState as? ProfileUiState.Success)?.isSelectionMode == true
    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelectionMode()
    }

    // 신고 다이얼로그
    if (showReportDialog) {
        com.route.readers.ui.components.ReportDialog(
            targetId = userId,
            targetType = "user",
            targetOwnerId = userId,
            onDismiss = { showReportDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White // Set background to White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White), // Ensure background is White
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> CircularProgressIndicator(color = Burgundy)
                is ProfileUiState.Error -> Text(text = state.message)
                is ProfileUiState.Success -> {
                    val levelInfo = calculateLevelInfo(state.user.totalPoints, state.user.level)
                    ProfileContent(
                        state = state,
                        levelInfo = levelInfo, // Pass LevelInfo object
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
                            viewModel.updateProfileImage(imageUri)
                        },
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToCustomization = onNavigateToCustomization,
                        onNavigateToMyBookList = onNavigateToMyBookList,
                        onNavigateToLevel = onNavigateToLevel,
                        onNavigateToGoal = onNavigateToGoal,
                        onNavigateToOtherUserProfile = onNavigateToOtherUserProfile,
                        onBlockUser = { viewModel.blockUser(state.user.uid) },
                        onUnblockUser = { viewModel.unblockUser(state.user.uid) },
                        onReportUser = { showReportDialog = true },
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
    levelInfo: LevelInfo, // Changed from level: Int
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
    onReportUser: () -> Unit = {},
    onBookmarkClick: (String, Boolean) -> Unit,
    onNavigateToOtherUserProfile: (String) -> Unit
) {
    val user = state.user
    val isPrivateAndNotFollowing = user.isPrivate && !state.isMyProfile && !state.isFollowing

    var showDeleteDialog by remember { mutableStateOf(false) }
    var feedToDelete by remember { mutableStateOf<String?>(null) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("피드", "관심도서", "업적")

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
        verticalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            ProfileInfoSection(
                user = user,
                levelInfo = levelInfo, // Pass levelInfo
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
                onUnblockUser = onUnblockUser,
                onReportUser = onReportUser
            )
        }

        if (state.isMyProfile) {
            item {
                AdBanner() // Ad banner above the GoalSettingSection
            }
            item {
                GoalSettingSection(
                    onSetGoalClick = onNavigateToGoal,
                    currentGoal = state.currentGoal
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
                    Text("차단된 사용자입니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (isPrivateAndNotFollowing) {
            item {
                PrivateProfileContent()
            }
        } else {
            stickyHeader {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White), // White background for tabs
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Burgundy,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Burgundy,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    Text(
                                        text = title, 
                                        fontSize = 15.sp, 
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = if (selectedTabIndex == index) FontFamily.Default else FontFamily.Serif
                                    ) 
                                },
                                selectedContentColor = Burgundy,
                                unselectedContentColor = Color.Gray
                            )
                        }
                    }
                }
            }

            // Contents of the tabs
            when (selectedTabIndex) {

                0 -> {
                    val myPosts = state.myPosts.filterIsInstance<FeedItem.BookReview>()
                    if (myPosts.isEmpty()) {
                        item {
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
                        }
                    } else {
                        items(myPosts) { post ->
                            FeedCard(
                                item = post,
                                isLiked = state.likedFeedIds.contains(post.id),
                                isBookmarked = state.bookmarkedFeedIds.contains(post.id),
                                onLikeClick = { viewModel.toggleLike(post.id, state.likedFeedIds.contains(post.id)) },
                                onBookmarkClick = { viewModel.toggleBookmark(post.id, state.bookmarkedFeedIds.contains(post.id)) },
                                onDeleteClick = {
                                    feedToDelete = post.id
                                    showDeleteDialog = true
                                },
                                onUserClick = {
                                    val userIdForProfile = (post as? FeedItem.BookReview)?.authorId
                                    if (userIdForProfile != null && userIdForProfile != state.user.uid) {
                                        onNavigateToOtherUserProfile(userIdForProfile)
                                    }
                                },
                                onFollowBack = { /* Not applicable for book reviews */ },
                                followerInfoMap = state.userInfoMap,
                                wishlist = state.wishlist,
                                myLibrary = state.myLibrary,
                                onToggleWishlist = { book, isInWishlist -> viewModel.toggleWishlist(book, isInWishlist) },
                                onToggleMyLibrary = { book, isInMyLibrary -> viewModel.toggleMyLibrary(book, isInMyLibrary) }
                            )
                        }
                    }
                }
                1 -> item {
                    FavoriteBooksSection(
                        books = state.favoriteBooks,
                        isSelectionMode = state.isSelectionMode,
                        selectedBookIds = state.selectedBookIds,
                        onToggleSelection = { viewModel.toggleBookSelection(it) },
                        onStartSelectionMode = { viewModel.startSelectionMode(it) },
                        onDeleteClick = { viewModel.deleteSelectedFavoriteBooks() },
                        onBookClick = { /* Navigate to book details */ },
                        onNavigateToSearch = onNavigateToSearch
                    )
                }
                2 -> item {
                    AchievementsSection(
                        ongoingAchievements = state.ongoingAchievements,
                        completedAchievements = state.completedAchievements,
                        onAchievementClick = { /* Navigate to achievement details */ }
                    )
                }
//                3 -> item {
//                    ChallengesSection(
//                        ongoingChallenges = state.ongoingChallenges,
//                        completedChallenges = state.completedChallenges,
//                        onChallengeClick = { /* Navigate to challenge details */ },
//                        userId = state.user.uid
//                    )
//                }
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
            containerColor = Color.Transparent,
            contentColor = Burgundy,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Burgundy
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { 
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = if (selectedTabIndex == index) FontFamily.Default else FontFamily.Serif
                        )
                    },
                    selectedContentColor = Burgundy,
                    unselectedContentColor = Color.Gray
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = Color.Transparent,
            contentColor = Burgundy,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                    color = Burgundy
                )
            },
            divider = {}
        ) {
            categories.forEachIndexed { index, title ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = { 
                        Text(
                            text = title,
                            fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = if (selectedCategoryIndex == index) FontFamily.Default else FontFamily.Serif
                        )
                    },
                    selectedContentColor = Burgundy,
                    unselectedContentColor = Color.Gray
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        fontFamily = FontFamily.Serif,
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

    // Premium Card Style
    val cardBackgroundColor = Color.White
    val cardBorder = if (isCompleted) BorderStroke(1.dp, Gold) else BorderStroke(1.dp, Color(0xFFEEEEEE))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if(isCompleted) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon Placeholder (Badge)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) Gold.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (isCompleted) Gold else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.title, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = achievement.description, 
                        fontSize = 13.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .background(Gold, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("달성 완료", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "진행도",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = if(isCompleted) Gold else Burgundy,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if(isCompleted) Gold else Burgundy,
                    trackColor = Color(0xFFF5F5F5)
                )
            }
        }
    }
}

@Composable
fun ProfileInfoSection(
    user: User,
    levelInfo: LevelInfo,
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
    onUnblockUser: () -> Unit,
    onReportUser: () -> Unit = {}
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            // Soft shadow
            .background(
                color = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFEEEEEE),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(1.dp) // inner border padding
    ) {
        // Gradient Card Background
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFAFAFA))
                ))
        )
        
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Menu
            Box(modifier = Modifier.fillMaxWidth()) {
                 // 더보기 메뉴 (다른 사용자 프로필일 때만)
                if (!isMyProfile) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            if (isFollowing) {
                                DropdownMenuItem(
                                    text = { Text("언팔로우") },
                                    onClick = {
                                        onUnfollowClick()
                                        showMoreMenu = false
                                    }
                                )
                            }
                            if (isBlocked) {
                                DropdownMenuItem(
                                    text = { Text("차단 해제") },
                                    onClick = {
                                        onUnblockUser()
                                        showMoreMenu = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("차단하기") },
                                    onClick = {
                                        onBlockUser()
                                        showMoreMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("신고하기", color = Burgundy) },
                                onClick = {
                                    onReportUser()
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Profile Image & Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.offset(y = (-20).dp) // Pull up slightly
            ) {
                ProfileImage(
                    user = user,
                    isMyProfile = isMyProfile,
                    onImageClick = {
                        onNavigateToCustomization()
                    }
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = user.nickname, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    user.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Burgundy,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Premium Level Chip & XP
                LevelChip(levelInfo = levelInfo, onClick = onNavigateToLevel)
            }

            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileInfoItem(count = user.followerCount.toString(), label = "팔로워", onClick = { onFollowListClick("followers") })
                // Divider
                Box(modifier = Modifier.size(1.dp, 24.dp).background(Color(0xFFEEEEEE)))
                ProfileInfoItem(count = user.followingCount.toString(), label = "팔로잉", onClick = { onFollowListClick("following") })
                // Divider
                Box(modifier = Modifier.size(1.dp, 24.dp).background(Color(0xFFEEEEEE)))
                ProfileInfoItem(
                    count = user.readBookCount.toString(),
                    label = "읽은 책",
                    onClick = if (isMyProfile) onNavigateToMyBookList else null
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ReadingStreakSection(
                attendanceDays = user.consecutiveDays,
                readingDays = user.consecutiveReadingDays
            )

            // 팔로우 버튼 (다른 사용자 프로필일 때만)
            if (!isMyProfile && !isBlocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { if (isFollowing) onUnfollowClick() else onFollowClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFFF5F5F5) else Burgundy,
                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isFollowing) 0.dp else 4.dp)
                ) {
                    Text(
                        text = if (isFollowing) "팔로잉" else "팔로우",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GoalSettingSection(
    onSetGoalClick: () -> Unit,
    currentGoal: Goal? = null
) {
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
            Column {
                if (currentGoal != null) {
                    val totalPages = currentGoal.pages.toIntOrNull() ?: 0
                    val progress = if (totalPages > 0) {
                        (currentGoal.currentPage.toFloat() / totalPages.toFloat())
                    } else 0f
                    
                    Text(
                        text = "현재 목표: ${currentGoal.bookTitle}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "하루 ${currentGoal.dailyPages}페이지 • ${currentGoal.duration}일",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.weight(1f),
                            color = DarkRed,
                            trackColor = DarkRed.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${currentGoal.currentPage}/${totalPages}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
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
            }
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
    val backgroundColor = try {
        user.profileBackgroundColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: DarkRed
    } catch (e: Exception) {
        DarkRed
    }

    Box(
        modifier = Modifier
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
    }
}

@Composable
fun LevelChip(levelInfo: LevelInfo, onClick: () -> Unit) {
    val currentXp = levelInfo.currentPoints
    val requiredXp = levelInfo.pointsForNextLevel
    val progress = if (requiredXp > 0) currentXp.toFloat() / requiredXp.toFloat() else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Level Badge
        Box(
            modifier = Modifier
                .border(1.dp, Gold, CircleShape)
                .background(Color.White, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "LV.${levelInfo.currentLevel}",
                    color = Burgundy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Sleek XP Progress Bar
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFEEEEEE))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Gold)
            )
        }
    }
}

@Composable
fun ProfileInfoItem(count: String, label: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(8.dp)) {
        Text(
            text = count, 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Default
        )
        Text(
            text = label.uppercase(), 
            fontSize = 11.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ReadingStreakSection(attendanceDays: Int, readingDays: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PremiumStreakItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.LocalFireDepartment,
            label = "출석 스트릭",
            days = attendanceDays,
            color = Color(0xFFE57373)
        )
        PremiumStreakItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.AutoStories,
            label = "독서 기록",
            days = readingDays,
            color = Color(0xFF81C784)
        )
    }
}

@Composable
fun PremiumStreakItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    days: Int,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9F9F9))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$days 일",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
    onChallengeClick: (Challenge) -> Unit,
    userId: String
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
            onChallengeClick = onChallengeClick,
            userId = userId
        )
        ChallengeCategory(
            title = "완료한 챌린지",
            challenges = completedChallenges,
            onChallengeClick = onChallengeClick,
            userId = userId
        )
    }
}

@Composable
fun ChallengeCategory(
    title: String,
    challenges: List<Challenge>,
    onChallengeClick: (Challenge) -> Unit,
    userId: String
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
                    ChallengeItem(challenge = challenge, onClick = { onChallengeClick(challenge) }, userId = userId)
                }
            }
        }
    }
}

@Composable
fun ChallengeItem(challenge: Challenge, onClick: () -> Unit, userId: String) {
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
                    val userProgress = (challenge.progress[userId] as? Number)?.toInt() ?: 0

                    val (currentProgressValue, totalGoalValue, progressUnit) = when (challenge.type) {
                        ChallengeType.DAILY_PAGES_READING -> {
                            val dailyGoalMetDays = challenge.dailyProgress[userId]?.values?.count { pages ->
                                (pages as? Number)?.toInt() ?: 0 >= challenge.goal
                            } ?: 0
                            Triple(dailyGoalMetDays, 7, "일")
                        }
                        ChallengeType.CONSECUTIVE_READING, ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> {
                            Triple(userProgress, 7, "일")
                        }
                        else -> {
                            Triple(userProgress, challenge.goal, "일")
                        }
                    }
                    val overallProgressFraction = if (totalGoalValue > 0) currentProgressValue.toFloat() / totalGoalValue.toFloat() else 0f
                    val progressPercent = (overallProgressFraction * 100).toInt().coerceAtMost(100)

                    Text(text = "$progressPercent%", color = DarkRed, fontWeight = FontWeight.Bold)
                }
            }
            Text(text = challenge.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val userProgress = (challenge.progress[userId] as? Number)?.toInt() ?: 0
            val (currentProgressValue, totalGoalValue, progressUnit) = when (challenge.type) {
                ChallengeType.DAILY_PAGES_READING -> {
                    val dailyGoalMetDays = challenge.dailyProgress[userId]?.values?.count { pages ->
                        (pages as? Number)?.toInt() ?: 0 >= challenge.goal
                    } ?: 0
                    Triple(dailyGoalMetDays, 7, "일")
                }
                ChallengeType.CONSECUTIVE_READING, ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> {
                    Triple(userProgress, 7, "일")
                }
                else -> {
                    Triple(userProgress, challenge.goal, "일")
                }
            }
            val progress = if (totalGoalValue > 0) currentProgressValue.toFloat() / totalGoalValue.toFloat() else 0f

            LinearProgressIndicator(
                progress = { progress },
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
