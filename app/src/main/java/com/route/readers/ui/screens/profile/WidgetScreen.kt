package com.route.readers.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: WidgetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("위젯 설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            WidgetPreview(uiState)

            WidgetCustomizationOptions(
                uiState = uiState,
                onStyleChange = viewModel::setWidgetStyle,
                onShowFriendReadingChange = viewModel::setShowFriendReading,
                onShowProgressBarChange = viewModel::setShowProgressBar,
                onShowPlayButtonChange = viewModel::setShowPlayButton,
                onColorSchemeChange = viewModel::setColorScheme
            )
        }
    }
}

@Composable
fun WidgetPreview(uiState: WidgetUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "위젯 미리보기",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (uiState.colorScheme) {
                        WidgetColorScheme.LIGHT -> MaterialTheme.colorScheme.surfaceVariant
                        WidgetColorScheme.DARK -> MaterialTheme.colorScheme.inverseSurface
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (uiState.style) {
                WidgetStyle.NORMAL -> NormalWidgetPreview(uiState)
                WidgetStyle.MINIMAL -> MinimalWidgetPreview(uiState)
            }
        }
    }
}

@Composable
fun NormalWidgetPreview(uiState: WidgetUiState) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("책 제목", fontWeight = FontWeight.Bold)
                if (uiState.showPlayButton) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text("저자", fontSize = 12.sp)
            if (uiState.showProgressBar) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = 0.6f)
            }
            if (uiState.showFriendReading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("친구가 이 책을 읽고 있어요!", fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun MinimalWidgetPreview(uiState: WidgetUiState) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("책 제목", fontWeight = FontWeight.Bold)
            if (uiState.showPlayButton) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        if (uiState.showProgressBar) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = 0.6f)
        }
    }
}

@Composable
fun WidgetCustomizationOptions(
    uiState: WidgetUiState,
    onStyleChange: (WidgetStyle) -> Unit,
    onShowFriendReadingChange: (Boolean) -> Unit,
    onShowProgressBarChange: (Boolean) -> Unit,
    onShowPlayButtonChange: (Boolean) -> Unit,
    onColorSchemeChange: (WidgetColorScheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "위젯 설정",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Widget Style
        Column {
            Text("스타일", fontWeight = FontWeight.SemiBold)
            Row {
                WidgetStyle.values().forEach { style ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.style == style,
                            onClick = { onStyleChange(style) }
                        )
                        Text(style.name)
                    }
                }
            }
        }

        // Show/Hide Elements
        Column {
            Text("표시 요소", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("친구 독서 활동 표시")
                Switch(
                    checked = uiState.showFriendReading,
                    onCheckedChange = onShowFriendReadingChange
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("진행률 표시")
                Switch(
                    checked = uiState.showProgressBar,
                    onCheckedChange = onShowProgressBarChange
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("플레이 버튼 표시")
                Switch(
                    checked = uiState.showPlayButton,
                    onCheckedChange = onShowPlayButtonChange
                )
            }
        }

        // Color Scheme
        Column {
            Text("색상 테마", fontWeight = FontWeight.SemiBold)
            Row {
                WidgetColorScheme.values().forEach { scheme ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.colorScheme == scheme,
                            onClick = { onColorSchemeChange(scheme) }
                        )
                        Text(scheme.name)
                    }
                }
            }
        }
    }
}
