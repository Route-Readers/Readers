package com.route.readers.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
// import androidx.compose.foundation.isSystemInDarkTheme // 사용 안 함
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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

@Composable
fun BottomNavBar(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem("피드", 0, selectedTab, onTabSelected)
        BottomNavItem("내서재", 1, selectedTab, onTabSelected)
        BottomNavItem("검색", 2, selectedTab, onTabSelected)
        BottomNavItem("커뮤니티", 3, selectedTab, onTabSelected)
        BottomNavItem("프로필", 4, selectedTab, onTabSelected)
    }
}

@Composable
fun BottomNavItem(
    text: String,
    index: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Text(
        text = text,
        color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clickable { onTabSelected(index) }
    )
}

val AppTypography = Typography()
val AppShapes = Shapes(medium = RoundedCornerShape(12.dp))

// 다크 모드 색상표 정의 제거
// private val DarkPinkColorScheme = darkColorScheme(...)

private val LightPinkColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8BBD0),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    background = Color(0xFFF0F0F0), // 밝은 배경색
    onBackground = Color.Black,   // 밝은 배경 위의 텍스트색
    surface = Color.White,        // 카드, 시트 등의 표면색
    onSurface = Color.Black,      // 표면 위의 텍스트색
    surfaceVariant = Color(0xFFEDEDED), // 표면 변형 색 (예: 연한 회색 카드 배경)
    onSurfaceVariant = Color.DarkGray,  // 표면 변형 위의 텍스트색
    error = Color(0xFFB00020),         // 에러 색상
    onError = Color.White             // 에러 색상 위의 텍스트색
)

@Composable
fun MyProfileAppTheme(
    // darkTheme 파라미터 및 관련 로직 제거
    content: @Composable () -> Unit
) {
    val colorScheme = LightPinkColorScheme // 항상 라이트 모드 색상표 사용

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun ProfileScreen() {
    MyProfileAppTheme { // MyProfileAppTheme은 이제 항상 라이트 모드
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePageComposable() {
    var selectedBottomTab by remember { mutableStateOf(4) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("마이 페이지", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    IconButton(onClick = {
                        Log.d("ProfilePage", "TopAppBar 설정 버튼 클릭")
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "환경설정")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedBottomTab,
                onTabSelected = { index ->
                    selectedBottomTab = index
                    Log.d("ProfilePage", "Bottom tab $index selected")
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ProfileSection() }
            item { MyActivitySection() }
            item { MyTokenSection() }
            item { ReadingStatsSection() }
            item { ReadingCalendarSection() }
            item { RecentBooksSection() }
            item { EventSection() }
            item { SettingsSection() }
            // 스크롤 시 하단바에 내용이 가려지지 않도록 마지막에 여백 추가 (선택 사항)
            // item { Spacer(modifier = Modifier.height(16.dp)) }
        }
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
                    .background(Color.LightGray) // 직접 색상 사용
                    .clickable { Log.d("Profile", "프로필 사진 변경 클릭") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "프로필 사진", tint = Color.DarkGray, modifier = Modifier.size(40.dp)) // 직접 색상 사용
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("홍길동님", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isMyOnlineStatus) Color(0xFF4CAF50) else Color.Red) // 직접 색상 사용
                            .clickable {
                                isMyOnlineStatus = !isMyOnlineStatus
                                Log.d("Profile", "상태 표시등 클릭: ${if (isMyOnlineStatus) "온라인" else "오프라인"}")
                            }
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape) // 직접 색상 사용
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("책과 함께하는 멋진 하루! 📚✨", fontSize = 14.sp, color = Color.Gray, maxLines = 2) // 직접 색상 사용
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Text("독서 취향", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        val genres = listOf("소설", "자기계발", "역사", "과학", "판타지", "에세이")
        var selectedGenres by remember { mutableStateOf(setOf<String>()) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)) {
            genres.take(3).forEach { genre ->
                GenreChip(genre, selectedGenres.contains(genre)) {
                    selectedGenres = if (selectedGenres.contains(genre)) selectedGenres - genre else selectedGenres + genre
                }
            }
            TextButton(onClick = { Log.d("Profile", "독서 취향 더보기 클릭") }) { Text("더보기") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("친구 ${friendsList.size}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            TextButton(onClick = { Log.d("Profile", "친구 추가 버튼 클릭") }) {
                Icon(Icons.Filled.Add, contentDescription = "친구 추가")
                Spacer(modifier = Modifier.width(4.dp))
                Text("추가")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (friendsList.isEmpty()) {
            Text("아직 친구가 없어요. 친구를 추가해보세요!", fontSize = 14.sp, color = Color.Gray) // 직접 색상 사용
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
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    .background(Color.LightGray) // 직접 색상 사용
                    .clickable { Log.d("Profile", "${friend.name} 프로필 클릭") },
                contentAlignment = Alignment.Center
            ) {
                Text(friend.name.first().toString(), fontSize = 24.sp, color = Color.DarkGray) // 직접 색상 사용
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (friend.isOnline) Color(0xFF4CAF50) else Color.Gray) // 직접 색상 사용
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(friend.name, fontSize = 12.sp, maxLines = 1)
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
        Text("나의 피드", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (myFeeds.isEmpty()) {
            Text("아직 작성한 피드가 없어요.", fontSize = 14.sp, color = Color.Gray) // 직접 색상 사용
        } else {
            myFeeds.firstOrNull()?.let { FeedItemView(it, true); Spacer(Modifier.height(8.dp)) }
            TextButton(onClick = { Log.d("MyActivity", "나의 피드 전체 보기 클릭") }, modifier = Modifier.fillMaxWidth()) { Text("내 피드 더보기") }
        }
        Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))
        Text("저장한 피드", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (savedFeedsFromOthers.isEmpty()) {
            Text("아직 저장한 피드가 없어요.", fontSize = 14.sp, color = Color.Gray) // 직접 색상 사용
        } else {
            savedFeedsFromOthers.firstOrNull()?.let { FeedItemView(it, false); Spacer(Modifier.height(8.dp)) }
            TextButton(onClick = { Log.d("MyActivity", "저장한 피드 전체 보기 클릭") }, modifier = Modifier.fillMaxWidth()) { Text("저장한 피드 더보기") }
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
                Text(if (isMyFeed) "나" else feed.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        tint = if (isFavoriteState) Color(0xFFFFC107) else Color.Gray, // 직접 색상 사용
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(android.text.format.DateUtils.getRelativeTimeSpanString(feed.timestamp).toString(), fontSize = 12.sp, color = Color.Gray) // 직접 색상 사용
            }
            Spacer(Modifier.height(4.dp))
            Text(feed.content, fontSize = 14.sp)
            feed.imageUrl?.let {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.LightGray) // 직접 색상 사용
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("이미지 자리: $it", fontSize = 12.sp, color = Color.DarkGray) // 직접 색상 사용
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
            Text("보유 토큰:", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("1,250 P", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { Log.d("MyToken", "토큰 내역 보기 버튼 클릭") }) { Text("내역") }
        }
    }
}

@Composable
fun ReadingStatsSection() {
    ProfileCard(title = "독서 통계", icon = Icons.Filled.BarChart) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("이번 달 완독", fontSize = 16.sp); Text("3 권", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("총 독서 시간", fontSize = 16.sp); Text("15시간 20분", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
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
                Icon(Icons.Filled.DateRange, contentDescription = "달력 보기")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("목표: 5권 / 완독: 3권", fontSize = 14.sp)
            Text("연속 독서: 7일", fontSize = 14.sp)
        }
    }
}

@Composable
fun RecentBooksSection() {
    ProfileCard(title = "최근 읽은 책", icon = Icons.AutoMirrored.Filled.LibraryBooks) {
        Text("최근 읽은 책 목록이 여기에 표시됩니다.", fontSize = 14.sp, color = Color.Gray) // 직접 색상 사용
        Spacer(Modifier.height(8.dp))
        Column { Text("- 책 제목 1 (저자 1)"); Text("- 책 제목 2 (저자 2)") }
    }
}

@Composable
fun EventSection() {
    ProfileCard(title = "이벤트", icon = Icons.Filled.CardGiftcard) {
        Text("진행 중인 이벤트 정보가 여기에 표시됩니다.", fontSize = 14.sp, color = Color.Gray) // 직접 색상 사용
        Spacer(Modifier.height(8.dp))
        Button(onClick = { Log.d("Event", "모든 이벤트 보기 클릭") }) { Text("모든 이벤트 보기") }
    }
}

@Composable
fun SettingsSection() {
    ProfileCard(title = "환경설정", icon = null) {
        Column {
            SettingsItem(title = "알림 설정", icon = Icons.Filled.Notifications) { Log.d("Settings", "알림 설정 클릭") }
            HorizontalDivider()
            SettingsItem(title = "계정 관리", icon = Icons.Filled.AccountCircle) { Log.d("Settings", "계정 관리 클릭") }
            HorizontalDivider()
            SettingsItem(title = "앱 정보") { Log.d("Settings", "앱 정보 클릭") }
        }
    }
}

@Composable
fun SettingsItem(title: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "더보기", tint = Color.Gray) // 직접 색상 사용
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
        shape = MaterialTheme.shapes.medium, // AppShapes.medium 사용 가능
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            content() // ColumnScope 내에서 호출됨 (원래 코드에서는 Column { content() } 였음)
            // content 람다 자체가 ColumnScope를 가지므로 직접 호출해도 무방할 수 있으나,
            // 명시적으로 Column { content() } 로 감싸는 것이 더 안전할 수 있습니다.
            // 여기서는 제공된 코드의 구조를 최대한 유지합니다.
        }
    }
}

// --- 미리보기 코드 ---
@Preview(showBackground = true, name = "Profile Screen Light", heightDp = 1200)
@Composable
fun DefaultProfileScreenPreview() {
    ProfileScreen() // ProfileScreen은 MyProfileAppTheme을 사용 (항상 라이트 모드)
}

// 다크 모드 미리보기 제거
// @Preview(showBackground = true, name = "Profile Screen Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, heightDp = 1200)
// @Composable
// fun DarkProfileScreenPreview() {
// ProfileScreen()
// }
