package com.route.readers.ui.screens.add_feed

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.route.readers.data.model.Book
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddFeedViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    LaunchedEffect(uiState) {
        if (uiState is AddFeedUiState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("피드 작성") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.submitFeed() },
                        enabled = uiState !is AddFeedUiState.Loading
                    ) {
                        Text("게시", color = DarkRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF7F7FF)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // 책 검색 및 선택 UI
                BookSelectionSection(viewModel = viewModel)
            }

            // 책이 선택된 경우에만 리뷰 작성 UI 표시
            item {
                AnimatedVisibility(
                    visible = viewModel.selectedBook != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ReviewInputSection(
                        reviewText = viewModel.reviewText,
                        onReviewChange = { viewModel.reviewText = it },
                        rating = viewModel.rating,
                        onRatingChange = { viewModel.rating = it }
                    )
                }
            }
        }

        if (uiState is AddFeedUiState.Loading) {
            Dialog(onDismissRequest = {}) {
                CircularProgressIndicator()
            }
        }

        if (uiState is AddFeedUiState.Error) {
            val context = LocalContext.current
            LaunchedEffect(uiState) {
                android.widget.Toast.makeText(context, (uiState as AddFeedUiState.Error).message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// 책 검색 및 선택을 담당하는 UI
@Composable
fun BookSelectionSection(viewModel: AddFeedViewModel) {
    Column {
        if (viewModel.selectedBook == null) {
            // 책 검색창
            OutlinedTextField(
                value = viewModel.searchText,
                onValueChange = { viewModel.onSearchTextChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("어떤 책에 대해 이야기하고 싶으신가요?") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                )
            )
        } else {
            // 선택된 책 정보 표시
            SelectedBookCard(
                book = viewModel.selectedBook!!,
                onClear = { viewModel.clearSelectedBook() }
            )
        }

        // 검색 로딩 인디케이터
        if (viewModel.isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        // 검색 결과 목록
        AnimatedVisibility(visible = viewModel.searchedBooks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(Color.White)
            ) {
                items(viewModel.searchedBooks, key = { it.isbn }) { book ->
                    BookSearchResultItem(
                        book = book,
                        onBookClick = { viewModel.onBookSelected(book) }
                    )
                }
            }
        }
    }
}

// 리뷰 내용과 별점을 입력하는 UI
@Composable
fun ReviewInputSection(
    reviewText: String,
    onReviewChange: (String) -> Unit,
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 별점 입력
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("이 책에 대한 별점을 매겨주세요", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                (1..5).forEach { index ->
                    IconButton(onClick = { onRatingChange(index) }) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "$index 점",
                            modifier = Modifier.size(36.dp),
                            tint = if (index <= rating) DarkRed else Color.LightGray
                        )
                    }
                }
            }
        }

        // 리뷰 텍스트 입력
        OutlinedTextField(
            value = reviewText,
            onValueChange = onReviewChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            label = { Text("리뷰를 작성해주세요.") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
            )
        )
    }
}

// 검색 결과 아이템 UI
@Composable
fun BookSearchResultItem(book: Book, onBookClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(book.cover)
                .crossfade(true)
                .build(),
            contentDescription = "책 표지",
            modifier = Modifier
                .height(60.dp)
                .width(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(book.author, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// 선택된 책 정보 카드 UI
@Composable
fun SelectedBookCard(book: Book, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.cover)
                    .crossfade(true)
                    .build(),
                contentDescription = book.title,
                modifier = Modifier
                    .height(120.dp)
                    .width(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(book.author, color = Color.DarkGray, fontSize = 14.sp)
            }
            IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "선택 취소")
            }
        }
    }
}
