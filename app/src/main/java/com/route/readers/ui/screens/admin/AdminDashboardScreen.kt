package com.route.readers.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.AdminLog
import com.route.readers.data.model.Report
import com.route.readers.data.model.User
import com.route.readers.ui.theme.DarkRed
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val context = LocalContext.current
    val pendingReports by viewModel.pendingReports.collectAsState()
    val adminLogs by viewModel.adminLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadPendingReports()
        viewModel.loadAdminLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("관리자 대시보드") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("신고 관리", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("유저 검색", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("활동 로그", modifier = Modifier.padding(16.dp))
                }
            }

            when (selectedTab) {
                0 -> ReportManagementTab(
                    reports = pendingReports,
                    isLoading = isLoading,
                    viewModel = viewModel,
                    onToast = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
                1 -> UserSearchTab(viewModel = viewModel)
                2 -> AdminLogTab(logs = adminLogs, isLoading = isLoading)
            }
        }
    }
}

@Composable
private fun ReportManagementTab(
    reports: List<Report>,
    isLoading: Boolean,
    viewModel: AdminViewModel,
    onToast: (String) -> Unit
) {
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    if (selectedReport != null) {
        ReportDetailSheet(
            report = selectedReport!!,
            onDismiss = { selectedReport = null },
            onWarnUser = {
                viewModel.warnUser(selectedReport!!.targetOwnerId, selectedReport!!.reason) { onToast("경고 완료") }
                viewModel.resolveReport(selectedReport!!.id, "warning") {}
                selectedReport = null
            },
            onBanUser = { days ->
                viewModel.banUser(selectedReport!!.targetOwnerId, selectedReport!!.reason, days) { onToast("정지 완료") }
                viewModel.resolveReport(selectedReport!!.id, "ban") {}
                selectedReport = null
            },
            onDeleteFeed = {
                if (selectedReport!!.targetType == "feed") {
                    viewModel.deleteFeed(selectedReport!!.targetId, selectedReport!!.reason) { onToast("피드 삭제 완료") }
                }
                viewModel.resolveReport(selectedReport!!.id, "delete") {}
                selectedReport = null
            },
            onDismissReport = {
                viewModel.dismissReport(selectedReport!!.id) { onToast("신고 기각") }
                selectedReport = null
            }
        )
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("대기 중인 신고가 없습니다", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reports) { report ->
                ReportCard(report = report, onClick = { selectedReport = report })
            }
        }
    }
}

@Composable
private fun ReportCard(report: Report, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (report.targetType) {
                            "user" -> Color(0xFF2196F3)
                            "feed" -> Color(0xFF4CAF50)
                            else -> Color(0xFFFF9800)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (report.targetType) {
                        "user" -> Icons.Default.Person
                        "feed" -> Icons.Default.Article
                        else -> Icons.Default.Chat
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (report.targetType) {
                        "user" -> "유저 신고"
                        "feed" -> "피드 신고"
                        else -> "채팅 신고"
                    },
                    fontWeight = FontWeight.Bold
                )
                Text(report.reason, fontSize = 13.sp, color = DarkRed)
                Text("신고자: ${report.reporterNickname}", fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(dateFormat.format(Date(report.createdAt)), fontSize = 11.sp, color = Color.Gray)
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDetailSheet(
    report: Report,
    onDismiss: () -> Unit,
    onWarnUser: () -> Unit,
    onBanUser: (Int?) -> Unit,
    onDeleteFeed: () -> Unit,
    onDismissReport: () -> Unit
) {
    var targetUserInfo by remember { mutableStateOf<User?>(null) }
    var targetUserFeeds by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoadingDetail by remember { mutableStateOf(true) }

    LaunchedEffect(report) {
        isLoadingDetail = true
        val db = FirebaseFirestore.getInstance()
        
        // 신고 대상 유저 정보 로드
        try {
            val userDoc = db.collection("users").document(report.targetOwnerId).get().await()
            targetUserInfo = userDoc.toObject(User::class.java)
            
            // 해당 유저의 최근 피드 로드
            val feedsSnapshot = db.collection("feeds")
                .whereEqualTo("authorId", report.targetOwnerId)
                .limit(5)
                .get().await()
            targetUserFeeds = feedsSnapshot.documents.map { doc ->
                mapOf(
                    "id" to doc.id,
                    "review" to (doc.getString("review") ?: ""),
                    "bookTitle" to (doc.getString("bookTitle") ?: ""),
                    "timestamp" to (doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L)
                )
            }
        } catch (e: Exception) {
            // 에러 무시
        }
        isLoadingDetail = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("신고 상세", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // 신고 정보
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("신고 사유: ${report.reason}", fontWeight = FontWeight.Medium)
                    report.reasonDetail?.let { Text("상세: $it", fontSize = 13.sp, color = Color.Gray) }
                    Text("신고자: ${report.reporterNickname}", fontSize = 13.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 신고 대상 유저 정보
            Text("신고 대상", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingDetail) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                targetUserInfo?.let { user ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(DarkRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.nickname.firstOrNull()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.nickname, fontWeight = FontWeight.Bold)
                                Text("경고 ${user.warnings}회 | ${if (user.isBanned) "정지됨" else "정상"}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // 최근 피드
                if (targetUserFeeds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("최근 피드", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    targetUserFeeds.forEach { feed ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(feed["bookTitle"] as String, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    feed["review"] as String,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 조치 버튼들
            Text("조치", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onWarnUser, modifier = Modifier.weight(1f)) {
                    Text("경고")
                }
                OutlinedButton(onClick = { onBanUser(7) }, modifier = Modifier.weight(1f)) {
                    Text("7일 정지")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onBanUser(null) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
                ) {
                    Text("영구 정지")
                }
                if (report.targetType == "feed") {
                    OutlinedButton(onClick = onDeleteFeed, modifier = Modifier.weight(1f)) {
                        Text("피드 삭제")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismissReport, modifier = Modifier.fillMaxWidth()) {
                Text("신고 기각", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UserSearchTab(viewModel: AdminViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("닉네임 또는 이메일") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { viewModel.searchUsers(searchQuery) }) {
                    Icon(Icons.Default.Search, "검색")
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { user ->
                    UserCard(
                        user = user,
                        onWarn = {
                            viewModel.warnUser(user.uid, "관리자 직접 경고") {
                                Toast.makeText(context, "경고 완료", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBan = { days ->
                            viewModel.banUser(user.uid, "관리자 직접 정지", days) {
                                Toast.makeText(context, "정지 완료", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUnban = {
                            viewModel.unbanUser(user.uid) {
                                Toast.makeText(context, "정지 해제 완료", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    onWarn: () -> Unit,
    onBan: (Int?) -> Unit,
    onUnban: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { showActions = !showActions },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(user.nickname, fontWeight = FontWeight.Bold)
                    Text(user.email ?: "이메일 없음", fontSize = 12.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (user.isBanned) {
                        Text("정지됨", color = DarkRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text("경고 ${user.warnings}회", fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (showActions) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user.isBanned) {
                        Button(onClick = onUnban, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                            Text("정지 해제")
                        }
                    } else {
                        OutlinedButton(onClick = onWarn) { Text("경고") }
                        OutlinedButton(onClick = { onBan(7) }) { Text("7일") }
                        Button(onClick = { onBan(null) }, colors = ButtonDefaults.buttonColors(containerColor = DarkRed)) {
                            Text("영구")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLogTab(logs: List<AdminLog>, isLoading: Boolean) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("활동 로그가 없습니다", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                log.action.contains("경고") -> Icons.Default.Warning
                                log.action.contains("정지") -> Icons.Default.Block
                                log.action.contains("삭제") -> Icons.Default.Delete
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when {
                                log.action.contains("경고") -> Color(0xFFFF9800)
                                log.action.contains("정지") -> DarkRed
                                log.action.contains("삭제") -> Color.Gray
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.action, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(log.details, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                            Text("by ${log.adminNickname}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(dateFormat.format(Date(log.timestamp)), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
