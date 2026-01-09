package com.route.readers.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.data.model.AdminLog
import com.route.readers.data.model.Report
import com.route.readers.data.model.User
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("신고 관리", "사용자 관리", "활동 로그")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("관리자 대시보드", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        when (selectedTab) {
                            0 -> viewModel.loadReports()
                            1 -> viewModel.clearUserSearch()
                            2 -> viewModel.loadAdminLogs()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ReportManagementTab(viewModel)
                1 -> UserManagementTab(viewModel)
                2 -> AdminLogsTab(viewModel)
            }
        }
    }
}

@Composable
fun ReportManagementTab(viewModel: AdminViewModel) {
    val reports by viewModel.reports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadReports() }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("신고 내역이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports) { report ->
                ReportCard(
                    report = report,
                    onResolve = { action -> viewModel.resolveReport(report.id, action) },
                    onDismiss = { viewModel.dismissReport(report.id) }
                )
            }
        }
    }
}

@Composable
fun UserManagementTab(viewModel: AdminViewModel) {
    val searchQuery by viewModel.userSearchQuery.collectAsState()
    val searchResults by viewModel.userSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onUserSearchQueryChanged(it) },
            label = { Text("닉네임 또는 이메일로 검색") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.searchUsers()
                keyboardController?.hide()
            })
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.searchUsers(); keyboardController?.hide() },
            modifier = Modifier.fillMaxWidth(),
            enabled = searchQuery.isNotBlank()
        ) {
            Text("검색")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { user ->
                    UserManagementCard(user = user, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun UserManagementCard(user: User, viewModel: AdminViewModel) {
    var showActionDialog by remember { mutableStateOf(false) }
    val isBanned = user.isBanned == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(user.nickname, fontWeight = FontWeight.Bold)
            Text("UID: ${user.uid.take(12)}...", style = MaterialTheme.typography.bodySmall)
            Text("이메일: ${user.email ?: "없음"}", style = MaterialTheme.typography.bodySmall)
            Text("경고: ${user.warnings ?: 0}회", style = MaterialTheme.typography.bodySmall)
            if (isBanned) {
                Text(
                    "상태: 정지됨 (${user.banReason ?: "사유 없음"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isBanned) {
                    Button(onClick = { viewModel.unbanUser(user.uid) }) {
                        Text("정지 해제")
                    }
                } else {
                    OutlinedButton(onClick = { showActionDialog = true }) {
                        Text("조치")
                    }
                }
            }
        }
    }

    if (showActionDialog) {
        UserActionDialog(
            user = user,
            onDismiss = { showActionDialog = false },
            onWarn = { reason -> viewModel.warnUser(user.uid, reason); showActionDialog = false },
            onBan = { reason, days -> viewModel.banUser(user.uid, reason, days); showActionDialog = false }
        )
    }
}

@Composable
fun UserActionDialog(
    user: User,
    onDismiss: () -> Unit,
    onWarn: (String) -> Unit,
    onBan: (String, Int?) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("warn") }
    var banDays by remember { mutableStateOf("7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${user.nickname}에게 조치") },
        text = {
            Column {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("사유") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedAction == "warn", onClick = { selectedAction = "warn" })
                    Text("경고")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedAction == "ban", onClick = { selectedAction = "ban" })
                    Text("정지")
                }

                if (selectedAction == "ban") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = banDays,
                        onValueChange = { banDays = it.filter { c -> c.isDigit() } },
                        label = { Text("정지 일수 (빈칸=영구)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedAction == "warn") onWarn(reason)
                    else onBan(reason, banDays.toIntOrNull())
                },
                enabled = reason.isNotBlank()
            ) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun AdminLogsTab(viewModel: AdminViewModel) {
    val logs by viewModel.adminLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadAdminLogs() }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("활동 로그가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log -> AdminLogCard(log) }
        }
    }
}

@Composable
fun AdminLogCard(log: AdminLog) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(log.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.bodySmall)
            }
            Text("관리자: ${log.adminNickname}", style = MaterialTheme.typography.bodySmall)
            Text(log.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReportCard(report: Report, onResolve: (String) -> Unit, onDismiss: () -> Unit) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    var showActionDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ID: ${report.id.take(8)}...", style = MaterialTheme.typography.bodySmall)
                StatusChip(status = report.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("사유: ${report.reason}", fontWeight = FontWeight.Medium)
            report.reasonDetail?.let { Text("상세: $it", style = MaterialTheme.typography.bodySmall) }
            Text("타입: ${report.targetType}", style = MaterialTheme.typography.bodySmall)
            Text("신고자: ${report.reporterNickname}", style = MaterialTheme.typography.bodySmall)
            Text("시간: ${dateFormat.format(Date(report.createdAt))}", style = MaterialTheme.typography.bodySmall)

            if (report.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showActionDialog = true }, modifier = Modifier.weight(1f)) { Text("조치") }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("기각") }
                }
            } else {
                Text(
                    if (report.status == "resolved") "처리됨: ${report.actionTaken}" else "기각됨",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    if (showActionDialog) {
        ActionDialog(onDismiss = { showActionDialog = false }, onAction = { onResolve(it); showActionDialog = false })
    }
}

@Composable
fun ActionDialog(onDismiss: () -> Unit, onAction: (String) -> Unit) {
    var selectedAction by remember { mutableStateOf("") }
    val actions = listOf("경고 발송", "콘텐츠 삭제", "정지 (1일)", "정지 (7일)", "정지 (30일)", "영구 정지")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("조치 선택") },
        text = {
            Column {
                actions.forEach { action ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedAction = action }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == action, onClick = { selectedAction = action })
                        Text(action)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAction(selectedAction) }, enabled = selectedAction.isNotEmpty()) { Text("확인") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun StatusChip(status: String) {
    val (text, color) = when (status) {
        "pending" -> "대기중" to MaterialTheme.colorScheme.error
        "resolved" -> "처리완료" to MaterialTheme.colorScheme.primary
        "dismissed" -> "기각됨" to MaterialTheme.colorScheme.outline
        else -> status to MaterialTheme.colorScheme.outline
    }
    Surface(color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}
