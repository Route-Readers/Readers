package com.example.register.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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



val Typography = androidx.compose.material3.Typography()
val Shapes = androidx.compose.material3.Shapes(medium = RoundedCornerShape(12.dp))


private val DarkPinkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFF06292),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFAD1457),
    onPrimaryContainer = Color(0xFFFFDDEB),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFCF6679),
    onError = Color.Black
)
private val LightPinkColorScheme = androidx.compose.material3.lightColorScheme(
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
    surfaceVariant = Color.LightGray,
    onSurfaceVariant = Color.DarkGray
)

@androidx.compose.runtime.Composable
fun MyProfileAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkPinkColorScheme
    } else {
        LightPinkColorScheme
    }

    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}


@androidx.compose.runtime.Composable
fun ProfileScreen() {
    MyProfileAppTheme {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            ProfilePageComposable()
        }
    }
}



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

data class TokenHistoryItem(
    val id: String,
    val type: String,
    val amount: Int,
    val description: String,
    val timestamp: Long
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun ProfilePageComposable() {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("마이 페이지", fontWeight = FontWeight.Bold) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
                actions = {
                    androidx.compose.material3.IconButton(onClick = {
                        Log.d("ProfilePage", "TopAppBar 설정 버튼 클릭")

                    }) {
                        androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "환경설정")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            item { ProfileSection() }
            item { MyActivitySection() }
            item { MyTokenSection() }
            item { ReadingStatsSection() }
            item { ReadingCalendarSection() }
            item { RecentBooksSection() }
            item { EventSection() }
            item { SettingsSection() }
        }
    }
}



@androidx.compose.runtime.Composable
fun ProfileSection() {
    var isMyOnlineStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    val friendsList = androidx.compose.runtime.remember {
        listOf(
            Friend(id = "1", name = "친구A", isOnline = true),
            Friend(id = "2", name = "친구B", isOnline = false),
            Friend(id = "3", name = "친구C", isOnline = true),
        )
    }

    ProfileCard(title = "프로필", icon = null, showEditButton = true, onEditClick = { Log.d("Profile", "프로필 편집 클릭") }) {
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { Log.d("Profile", "프로필 사진 변경 클릭") },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(Icons.Filled.PhotoCamera, contentDescription = "프로필 사진", tint = Color.DarkGray, modifier = Modifier.size(40.dp))
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text("홍길동님", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isMyOnlineStatus) Color(0xFF4CAF50) else Color.Red)
                            .clickable {
                                isMyOnlineStatus = !isMyOnlineStatus
                                Log.d(
                                    "Profile",
                                    "상태 표시등 클릭: ${if (isMyOnlineStatus) "온라인" else "오프라인"}"
                                )
                            }
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text("책과 함께하는 멋진 하루! 📚✨", fontSize = 14.sp, color = Color.Gray, maxLines = 2)
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.HorizontalDivider()
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Text("독서 취향", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        val genres = listOf("소설", "자기계발", "역사", "과학", "판타지", "에세이")
        var selectedGenres by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(setOf<String>()) }
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp, Alignment.Start)) {
            genres.take(3).forEach { genre ->
                GenreChip(genre, selectedGenres.contains(genre)) {
                    selectedGenres = if (selectedGenres.contains(genre)) selectedGenres - genre else selectedGenres + genre
                }
            }
            androidx.compose.material3.TextButton(onClick = { Log.d("Profile", "독서 취향 더보기 클릭") }) { androidx.compose.material3.Text("더보기") }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.HorizontalDivider()
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            androidx.compose.material3.Text("친구 ${friendsList.size}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            androidx.compose.material3.TextButton(onClick = { Log.d("Profile", "친구 추가 버튼 클릭") }) {
                androidx.compose.material3.Icon(Icons.Filled.Add, contentDescription = "친구 추가")
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.material3.Text("추가")
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        if (friendsList.isEmpty()) {
            androidx.compose.material3.Text("아직 친구가 없어요. 친구를 추가해보세요!", fontSize = 14.sp, color = Color.Gray)
        } else {
            LazyRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 8.dp)) {
                items(friendsList) { friend -> FriendItem(friend) }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun GenreChip(text: String, selected: Boolean, onChipClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onChipClick,
        label = { androidx.compose.material3.Text(text) },
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@androidx.compose.runtime.Composable
fun FriendItem(friend: Friend) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(
        androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        androidx.compose.foundation.layout.Box {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { Log.d("Profile", "${friend.name} 프로필 클릭") },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(friend.name.first().toString(), fontSize = 24.sp, color = Color.DarkGray)
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (friend.isOnline) Color(0xFF4CAF50) else Color.Gray)
                    .border(2.dp,
                        androidx.compose.material3.MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.material3.Text(friend.name, fontSize = 12.sp, maxLines = 1)
    }
}

@androidx.compose.runtime.Composable
fun MyActivitySection() {
    val myFeeds = androidx.compose.runtime.remember {
        listOf(
            FeedItem("my_feed_3", "홍길동", "오늘 날씨 정말 좋다! #일상", System.currentTimeMillis() - 100000, imageUrl = "https://example.com/image3.jpg", isFavorite = true),
            FeedItem("my_feed_2", "홍길동", "새로운 책 읽기 시작! 📚", System.currentTimeMillis() - 200000),
            FeedItem("my_feed_1", "홍길동", "첫 번째 게시글입니다~", System.currentTimeMillis() - 300000, imageUrl = "https://example.com/image1.jpg")
        ).sortedByDescending { it.timestamp }
    }
    val savedFeedsFromOthers = androidx.compose.runtime.remember {
        listOf(
            FeedItem("saved_feed_2", "작가B", "인상 깊은 구절 공유합니다.", System.currentTimeMillis() - 50000, isFavorite = true),
            FeedItem("saved_feed_1", "친구A", "이 책 추천해요! 정말 재밌음!", System.currentTimeMillis() - 150000, imageUrl = "https://example.com/image_friend.jpg", isFavorite = true)
        ).sortedByDescending { it.timestamp }
    }

    ProfileCard(title = "나의 활동", icon = Icons.Filled.Analytics) {
        androidx.compose.material3.Text("나의 피드", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        if (myFeeds.isEmpty()) {
            androidx.compose.material3.Text("아직 작성한 피드가 없어요.", fontSize = 14.sp, color = Color.Gray)
        } else {
            myFeeds.firstOrNull()?.let { FeedItemView(it, true); androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp)) }
            androidx.compose.material3.TextButton(onClick = { Log.d("MyActivity", "나의 피드 전체 보기 클릭") }, modifier = Modifier.fillMaxWidth()) { androidx.compose.material3.Text("내 피드 더보기") }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp)); androidx.compose.material3.HorizontalDivider(); androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Text("저장한 피드", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        if (savedFeedsFromOthers.isEmpty()) {
            androidx.compose.material3.Text("아직 저장한 피드가 없어요.", fontSize = 14.sp, color = Color.Gray)
        } else {
            savedFeedsFromOthers.firstOrNull()?.let { FeedItemView(it, false); androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp)) }
            androidx.compose.material3.TextButton(onClick = { Log.d("MyActivity", "저장한 피드 전체 보기 클릭") }, modifier = Modifier.fillMaxWidth()) { androidx.compose.material3.Text("저장한 피드 더보기") }
        }
    }
}

@androidx.compose.runtime.Composable
fun FeedItemView(feed: FeedItem, isMyFeed: Boolean) {
    var isFavoriteState by androidx.compose.runtime.remember(feed.id) { androidx.compose.runtime.mutableStateOf(feed.isFavorite) }
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(12.dp)) {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Text(if (isMyFeed) "나" else feed.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                androidx.compose.material3.IconButton(
                    onClick = {
                        isFavoriteState = !isFavoriteState; feed.isFavorite = isFavoriteState
                        Log.d("FeedItemView", "즐겨찾기: ${feed.id}, 상태: $isFavoriteState")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    androidx.compose.material3.Icon(if (isFavoriteState) Icons.Filled.Star else Icons.Filled.StarBorder, "즐겨찾기",
                        tint = if (isFavoriteState) Color(0xFFFFC107) else Color.Gray, modifier = Modifier.size(18.dp))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                androidx.compose.material3.Text(android.text.format.DateUtils.getRelativeTimeSpanString(feed.timestamp).toString(), fontSize = 12.sp, color = Color.Gray)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
            androidx.compose.material3.Text(feed.content, fontSize = 14.sp)
            feed.imageUrl?.let {
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.Box(Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
                    .clip(RoundedCornerShape(4.dp)), Alignment.Center) {
                    androidx.compose.material3.Text("이미지 자리: $it", fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun MyTokenSection() {
    ProfileCard(
        title = "나의 토큰",
        icon = Icons.Filled.Redeem,
        additionalActions = {
            androidx.compose.material3.IconButton(onClick = { Log.d("MyToken", "토큰 추가/충전 버튼 클릭") }) {
                androidx.compose.material3.Icon(Icons.Filled.AddCircleOutline, "토큰 충전", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
        }
    ) {
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Text("보유 토큰:", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Text("1,250 P", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = { Log.d("MyToken", "토큰 내역 보기 버튼 클릭") }) { androidx.compose.material3.Text("내역") }
        }
    }
}

@androidx.compose.runtime.Composable
fun ReadingStatsSection() {
    ProfileCard(title = "독서 통계", icon = Icons.Filled.BarChart) {
        androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(),
                androidx.compose.foundation.layout.Arrangement.SpaceBetween) { androidx.compose.material3.Text("이번 달 완독", fontSize = 16.sp); androidx.compose.material3.Text("3 권", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(),
                androidx.compose.foundation.layout.Arrangement.SpaceBetween) { androidx.compose.material3.Text("총 독서 시간", fontSize = 16.sp); androidx.compose.material3.Text("15시간 20분", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@androidx.compose.runtime.Composable
fun ReadingCalendarSection() {
    ProfileCard(
        title = "독서 캘린더",
        icon = Icons.Filled.CalendarToday,
        additionalActions = {
            androidx.compose.material3.IconButton(onClick = { Log.d("ReadingCalendar", "달력 보기 버튼 클릭") }) {
                androidx.compose.material3.Icon(Icons.Filled.DateRange, contentDescription = "달력 보기")
            }
        }
    ) {
        androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Text("목표: 5권 / 완독: 3권", fontSize = 14.sp)
            androidx.compose.material3.Text("연속 독서: 7일", fontSize = 14.sp)
        }
    }
}

@androidx.compose.runtime.Composable
fun RecentBooksSection() {
    ProfileCard(title = "최근 읽은 책", icon = Icons.AutoMirrored.Filled.LibraryBooks) {
        androidx.compose.material3.Text("최근 읽은 책 목록이 여기에 표시됩니다.", fontSize = 14.sp, color = Color.Gray)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.Column { androidx.compose.material3.Text("- 책 제목 1 (저자 1)"); androidx.compose.material3.Text("- 책 제목 2 (저자 2)") }
    }
}

@androidx.compose.runtime.Composable
fun EventSection() {
    ProfileCard(title = "이벤트", icon = Icons.Filled.CardGiftcard) {
        androidx.compose.material3.Text("진행 중인 이벤트 정보가 여기에 표시됩니다.", fontSize = 14.sp, color = Color.Gray)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Button(onClick = { Log.d("Event", "모든 이벤트 보기 클릭") }) { androidx.compose.material3.Text("모든 이벤트 보기") }
    }
}


@androidx.compose.runtime.Composable
fun SettingsSection() {
    ProfileCard(title = "환경설정", icon = null) {
        androidx.compose.foundation.layout.Column {
            SettingsItem(title = "알림 설정", icon = Icons.Filled.Notifications) { Log.d("Settings", "알림 설정 클릭") }
            androidx.compose.material3.HorizontalDivider()
            SettingsItem(title = "계정 관리", icon = Icons.Filled.AccountCircle) { Log.d("Settings", "계정 관리 클릭") }
            androidx.compose.material3.HorizontalDivider()
            SettingsItem(title = "앱 정보") { Log.d("Settings", "앱 정보 클릭") }
        }
    }
}

@androidx.compose.runtime.Composable
fun SettingsItem(title: String, icon: ImageVector? = null, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            androidx.compose.material3.Icon(it, title, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
        }
        androidx.compose.material3.Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "더보기", tint = Color.Gray)
    }
}

@androidx.compose.runtime.Composable
fun ProfileCard(
    title: String,
    icon: ImageVector? = null,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {},
    additionalActions: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium, // 여기서는 MaterialTheme.shapes를 직접 사용
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    icon?.let {
                        androidx.compose.material3.Icon(it, title, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary); androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    }
                    androidx.compose.material3.Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                }
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    additionalActions()
                    if (showEditButton) {
                        androidx.compose.material3.TextButton(onClick = onEditClick) {
                            androidx.compose.material3.Icon(Icons.Filled.Edit, "편집", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary); androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                            androidx.compose.material3.Text("편집", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            content()
        }
    }
}


// --- 미리보기 코드 ---
@Preview(showBackground = true, name = "Profile Screen Light")
@androidx.compose.runtime.Composable
fun DefaultProfileScreenPreview() {
    ProfileScreen()
}

@Preview(showBackground = true, name = "Profile Screen Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@androidx.compose.runtime.Composable
fun DarkProfileScreenPreview() {
    ProfileScreen()
}

