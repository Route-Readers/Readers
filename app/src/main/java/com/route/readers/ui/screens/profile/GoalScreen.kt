package com.route.readers.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.data.model.MyBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    onBack: () -> Unit,
    goalViewModel: GoalViewModel = viewModel()
) {
    val uiState by goalViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("목표 설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.showGoalInputs && !uiState.isLoading) {
                FloatingActionButton(onClick = { goalViewModel.onShowGoalInputs(true) }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "새로운 목표 추가하기")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.showGoalInputs) {
                GoalInputView(
                    uiState = uiState,
                    onMyBookSelected = { goalViewModel.onMyBookSelected(it) },
                    onDurationChange = { goalViewModel.onDurationChange(it) },
                    onPagesChange = { goalViewModel.onPagesChange(it) },
                    onSaveClick = { goalViewModel.saveGoal() }
                )
            } else {
                if (uiState.goals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { goalViewModel.onShowGoalInputs(true) }) {
                            Text("새로운 목표 추가하기")
                        }
                    }
                } else {
                    GoalListView(goals = uiState.goals)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalInputView(
    uiState: GoalUiState,
    onMyBookSelected: (MyBook) -> Unit,
    onDurationChange: (String) -> Unit,
    onPagesChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.myBooks.isEmpty()) {
            Text("목표로 설정할 읽는 중인 책이 없습니다.\n먼저 내 서재에 책을 추가해주세요.")
        } else {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedMyBook?.title ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("책 선택") },
                    placeholder = { Text("내 서재에서 책 선택하기") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    uiState.myBooks.forEach { book ->
                        DropdownMenuItem(
                            text = { Text(book.title, maxLines = 2) },
                            onClick = {
                                onMyBookSelected(book)
                                expanded = false
                            },
                            leadingIcon = {
                                AsyncImage(
                                    model = book.cover,
                                    contentDescription = book.title,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .width(32.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        )
                    }
                }
            }
        }

        if (uiState.selectedMyBook != null) {
            GoalInputTextField(
                label = "기간 설정",
                value = uiState.durationInput,
                onValueChange = onDurationChange,
                placeholder = "예: 30일"
            )
            GoalInputTextField(
                label = "페이지 설정",
                value = uiState.pagesInput,
                onValueChange = onPagesChange,
                placeholder = "총 페이지"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSaveClick, enabled = uiState.durationInput.isNotBlank() && uiState.pagesInput.isNotBlank()) {
                Text("목표 저장하기")
            }
        }
    }
}

@Composable
fun GoalListView(goals: List<Goal>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(goals) { goal ->
            GoalItem(goal = goal)
        }
    }
}

@Composable
fun GoalItem(goal: Goal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = goal.bookCover,
                contentDescription = goal.bookTitle,
                modifier = Modifier
                    .height(90.dp)
                    .width(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                GoalDetailRow(label = "책", detail = goal.bookTitle)
                Spacer(modifier = Modifier.height(8.dp))
                GoalDetailRow(label = "기간", detail = goal.duration)
                Spacer(modifier = Modifier.height(8.dp))
                GoalDetailRow(label = "분량", detail = goal.pages)
            }
        }
    }
}

@Composable
fun GoalDetailRow(label: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun GoalInputTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.LightGray,
        ),
        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
    )
}
