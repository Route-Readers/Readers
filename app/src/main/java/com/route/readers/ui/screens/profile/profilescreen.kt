package com.route.readers.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController

data class Friend(
    val id: String,
    val name: String,
    val profileImageUrl: String? = null,
    val isOnline: Boolean
)

data class FeedItem(
    val id: String,
    val authorName: String,
    val content: String,
    val timestamp: Long,
    val imageUrl: String? = null,
    var isFavorite: Boolean = false
)

val AppTypography = Typography()
val AppShapes = Shapes(medium = RoundedCornerShape(12.dp))

private val LightPinkColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8BBD0),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    background = Color(0xFFF0F0F0),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color.DarkGray,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun ReadersProfileAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightPinkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    bottomNavController: NavHostController?,
    appNavController: NavHostController?
) {
    ReadersProfileAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("마이 페이지", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    actions = {
                        IconButton(onClick = {
                            Log.d("ProfileScreen", "TopAppBar 설정 버튼 클릭")
                            bottomNavController?.navigate("settings_screen_route")
                        }) {
                            Icon(Icons.Filled.Settings, contentDescription = "환경설정")
                        }
                    }
                )
            }
        ) { innerPadding ->
            ProfilePageContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
fun ProfilePageContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item { ProfileSection() }
        item { MyActivitySection() }
        item { MyTokenSection() }
        item { ReadingStatsSection() }
        item { ReadingCalendarSection() }
        item { RecentBooksSection() }
        item { EventSection() }
    }
}

@Composable
fun ProfileSection() {
    var isMyOnlineStatus by remember { mutableStateOf(true) }
    val friendsList = remember {
        listOf(
            Friend(id = "1", name = "친구A", isOnline = true),
            Friend(id = "2", name = "친구B", isOnline = false),
            Friend(id = "3", name = "친구C", isOnline = true),
        )
    }

    ProfileCard(title = "프로필", icon = null, showEditButton = true, onEditClick = { Log.d("Profile", "프로필 편집 클릭") }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { Log.d("Profile", "프로필 사진 변경 클릭") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "프로필 사진", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("홍길동님", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isMyOnlineStatus) Color(0xFF4CAF50) else Color.Red)
                            .clickable {
                                isMyOnlineStatus = !isMyOnlineStatus
                                Log.d("Profile", "상태 표시등 클릭: ${if (isMyOnlineStatus) "온라인" else "오프라인"}")
                            }
                            .border(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("책과 함께하는 멋진 하루! 📚✨", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("독서 취향", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        val genres = listOf("소설", "자기계발", "역사", "과학", "판타지", "에세이")
        var selectedGenres by remember { mutableStateOf(setOf<String>()) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)) {
            genres.take(3).forEach { genre ->
                GenreChip(genre, selectedGenres.contains(genre)) {
                    selectedGenres = if (selectedGenres.contains(genre)) selectedGenres - genre else selectedGenres + genre
                }
            }
            TextButton(onClick = { Log.d("Profile", "독서 취향 더보기 클릭") }) { Text("더보기", color = MaterialTheme.colorScheme.primary) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("친구 ${friendsList.size}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            TextButton(onClick = { Log.d("Profile", "친구 추가 버튼 클릭") }) {
                Icon(Icons.Filled.Add, contentDescription = "친구 추가", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("추가", color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (friendsList.isEmpty()) {
            Text("아직 친구가 없어요. 친구를 추가해보세요!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) {
                items(friendsList) { friend -> FriendItem(friend) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreChip(text: String, selected: Boolean, onChipClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onChipClick,
        label = { Text(text) },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )
}

@Composable
fun FriendItem(friend: Friend) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(IntrinsicSize.Min)) {
        Box {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { Log.d("Profile", "${friend.name} 프로필 클릭") },
                contentAlignment = Alignment.Center
            ) {
                Text(friend.name.firstOrNull()?.toString() ?: "?", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (friend.isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(friend.name, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MyActivitySection() {
    val myFeeds = remember {
        listOf(
            FeedItem("my_feed_3", "홍길동", "오늘 날씨 정말 좋다! #일상", System.currentTimeMillis() - 100000, imageUrl = "https://example.com/image3.jpg", isFavorite = true),
            FeedItem("my_feed_2", "홍길동", "새로운 책 읽기 시작! 📚", System.currentTimeMillis() - 200000),
            FeedItem("my_feed_1", "홍길동", "첫 번째 게시글입니다~", System.currentTimeMillis() - 300000, imageUrl = "https://example.com/image1.jpg")
        ).sortedByDescending { it.timestamp }
    }
    val savedFeedsFromOthers = remember {
        listOf(
            FeedItem("saved_feed_2", "작가B", "인상 깊은 구절 공유합니다.", System.currentTimeMillis() - 50000, isFavorite = true),
            FeedItem("saved_feed_1", "친구A", "이 책 추천해요! 정말 재밌음!", System.currentTimeMillis() - 150000, imageUrl = "https://example.com/image_friend.jpg", isFavorite = true)
        ).sortedByDescending { it.timestamp }
    }

    ProfileCard(title = "나의 활동", icon = Icons.Filled.Analytics) {
        Text("나의 피드", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        if (myFeeds.isEmpty()) {
            Text("아직 작성한 피드가 없어요.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            myFeeds.firstOrNull()?.let { FeedItemView(it, true); Spacer(Modifier.height(8.dp)) }
            TextButton(onClick = { Log.d("MyActivity", "나의 피드 전체 보기 클릭") }, modifier = Modifier.fillMaxWidth()) { Text("내 피드 더보기", color = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(16.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)); Spacer(Modifier.height(16.dp))
        Text("저장한 피드", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        if (savedFeedsFromOthers.isEmpty()) {
            Text("아직 저장한 피드가 없어요.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            savedFeedsFromOthers.firstOrNull()?.let { FeedItemView(it, false); Spacer(Modifier.height(8.dp)) }
            TextButton(onClick = { Log.d("MyActivity", "저장한 피드 전체 보기 클릭") }, modifier = Modifier.fillMaxWidth()) { Text("저장한 피드 더보기", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun FeedItemView(feed: FeedItem, isMyFeed: Boolean) {
    var isFavoriteState by remember(feed.id) { mutableStateOf(feed.isFavorite) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isMyFeed) "나" else feed.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                IconButton(
                    onClick = {
                        isFavoriteState = !isFavoriteState
                        feed.isFavorite = isFavoriteState
                        Log.d("FeedItemView", "즐겨찾기: ${feed.id}, 상태: $isFavoriteState")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isFavoriteState) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "즐겨찾기",
                        tint = if (isFavoriteState) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    android.text.format.DateUtils.getRelativeTimeSpanString(feed.timestamp).toString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(feed.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            feed.imageUrl?.let {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("이미지 자리: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MyTokenSection() {
    ProfileCard(
        title = "나의 토큰",
        icon = Icons.Filled.Redeem,
        additionalActions = {
            IconButton(onClick = { Log.d("MyToken", "토큰 추가/충전 버튼 클릭") }) {
                Icon(Icons.Filled.AddCircleOutline, "토큰 충전", tint = MaterialTheme.colorScheme.primary)
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("보유 토큰:", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(8.dp))
            Text("1,250 P", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { Log.d("MyToken", "토큰 내역 보기 버튼 클릭") }) { Text("내역", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun ReadingStatsSection() {
    ProfileCard(title = "독서 통계", icon = Icons.Filled.BarChart) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("이번 달 완독", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("3 권", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("총 독서 시간", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("15시간 20분", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
fun ReadingCalendarSection() {
    ProfileCard(
        title = "독서 캘린더",
        icon = Icons.Filled.CalendarToday,
        additionalActions = {
            IconButton(onClick = { Log.d("ReadingCalendar", "달력 보기 버튼 클릭") }) {
                Icon(Icons.Filled.DateRange, contentDescription = "달력 보기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("목표: 5권 / 완독: 3권", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Text("연속 독서: 7일", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun RecentBooksSection() {
    ProfileCard(title = "최근 읽은 책", icon = Icons.AutoMirrored.Filled.LibraryBooks) {
        Text("최근 읽은 책 목록이 여기에 표시됩니다.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Column {
            Text("- 책 제목 1 (저자 1)", color = MaterialTheme.colorScheme.onBackground)
            Text("- 책 제목 2 (저자 2)", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun EventSection() {
    ProfileCard(title = "이벤트", icon = Icons.Filled.CardGiftcard) {
        Text("진행 중인 이벤트 정보가 여기에 표시됩니다.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { Log.d("Event", "모든 이벤트 보기 클릭") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("모든 이벤트 보기", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
@Composable
fun ProfileCard(
    title: String,
    icon: ImageVector? = null,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {},
    additionalActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    icon?.let {
                        Icon(it, title, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp))
                    }
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    additionalActions()
                    if (showEditButton) {
                        TextButton(onClick = onEditClick) {
                            Icon(Icons.Filled.Edit, "편집", tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(4.dp))
                            Text("편집", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            this.content()
        }
    }
}

@Preview(showBackground = true, name = "Profile Screen Full", heightDp = 1600)
@Composable
fun DefaultProfileScreenPreview() {
    ReadersProfileAppTheme{
        ProfileScreen(
            bottomNavController = rememberNavController(),
            appNavController = rememberNavController()
        )
    }
}
