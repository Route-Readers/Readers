package com.route.readers.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.data.model.Book

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
                    onSearchQueryChange = { goalViewModel.onSearchQueryChange(it) },
                    onBookSelected = { goalViewModel.onBookSelected(it) },
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

@Composable
fun GoalInputView(
    uiState: GoalUiState,
    onSearchQueryChange: (String) -> Unit,
    onBookSelected: (Book) -> Unit,
    onDurationChange: (String) -> Unit,
    onPagesChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 책 검색 필드
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("책 검색") },
            placeholder = { Text("목표로 할 책을 검색하세요") },
            singleLine = true
        )

        // 검색 중 인디케이터 또는 검색 결과 목록
        if (uiState.isSearching) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
        } else if (uiState.searchResults.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(uiState.searchResults) { book ->
                    BookSearchItem(book = book, onBookSelected = onBookSelected)
                }
            }
        }

        // 기간 및 페이지 설정 (책이 선택된 후에 활성화)
        if (uiState.selectedBook != null) {
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
            Button(onClick = onSaveClick) {
                Text("목표 저장하기")
            }
        }
    }
}

@Composable
fun BookSearchItem(book: Book, onBookSelected: (Book) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookSelected(book) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = book.cover,
            contentDescription = book.title,
            modifier = Modifier
                .height(60.dp)
                .width(40.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = book.title, style = MaterialTheme.typography.bodyLarge)
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
