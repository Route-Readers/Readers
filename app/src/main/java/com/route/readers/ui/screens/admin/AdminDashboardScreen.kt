package com.route.readers.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.data.model.Report
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val reports by viewModel.reports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                }
                Text(
                    text = "신고 관리",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(onClick = { viewModel.loadReports() }) {
                Icon(Icons.Default.Refresh, contentDescription = "새로고침")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports) { report ->
                    ReportCard(
                        report = report,
                        onResolve = { actionTaken ->
                            viewModel.resolveReport(report.id, actionTaken)
                        },
                        onDismiss = {
                            viewModel.dismissReport(report.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    report: Report,
    onResolve: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var showActionDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "신고 ID: ${report.id.take(8)}...",
                    style = MaterialTheme.typography.bodySmall
                )
                StatusChip(status = report.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "신고 이유: ${report.reason}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            if (!report.reasonDetail.isNullOrBlank()) {
                Text(
                    text = "상세 사유: ${report.reasonDetail}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Text(
                text = "타입: ${report.targetType}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "신고자: ${report.reporterNickname} (${report.reporterId.take(8)}...)",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "신고 시간: ${dateFormat.format(Date(report.createdAt))}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (report.status == "pending") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showActionDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("조치 취하기")
                    }
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("기각")
                    }
                }
            } else {
                Text(
                    text = when (report.status) {
                        "resolved" -> "처리 완료: ${report.actionTaken ?: "조치 취함"}"
                        "dismissed" -> "기각됨"
                        else -> "상태: ${report.status}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
    
    if (showActionDialog) {
        ActionDialog(
            onDismiss = { showActionDialog = false },
            onAction = { action ->
                onResolve(action)
                showActionDialog = false
            }
        )
    }
}

@Composable
fun ActionDialog(
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    var selectedAction by remember { mutableStateOf("") }
    
    val actions = listOf(
        "경고 발송",
        "콘텐츠 삭제",
        "사용자 정지 (1일)",
        "사용자 정지 (7일)",
        "사용자 정지 (30일)",
        "영구 정지"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("조치 선택") },
        text = {
            Column {
                Text("취할 조치를 선택하세요:")
                Spacer(modifier = Modifier.height(8.dp))
                actions.forEach { action ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action }
                        )
                        Text(action)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(selectedAction) },
                enabled = selectedAction.isNotEmpty()
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
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
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
