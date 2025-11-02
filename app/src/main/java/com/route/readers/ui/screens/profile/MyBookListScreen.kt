package com.route.readers.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.Book
import com.route.readers.ui.theme.DarkRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookListScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var expandedBookIsbn by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = currentUserId) {
        if (currentUserId != null) {
            viewModel.fetchUserProfile(currentUserId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("읽은 책 목록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DarkRed
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is ProfileUiState.Success -> {
                    // MyBook의 categoryName으로 그룹화하는 로직 복원
                    val booksByGenre = state.readBooks.groupBy { myBook ->
                        // Aladin API의 카테고리 이름 형식 "국내도서>소설/시/희곡"에서 마지막 부분만 추출
                        myBook.categoryName?.split(">")?.lastOrNull()?.trim() ?: "기타"
                    }

                    if (state.readBooks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "아직 완독한 책이 없어요.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // 사용자의 선호 장르를 먼저 정렬하고, 그 외 장르를 가나다순으로 정렬
                            val preferredGenres = state.user.readingGenres
                            val remainingGenres = (booksByGenre.keys - preferredGenres.toSet()).sorted()
                            val sortedGenres = preferredGenres + remainingGenres

                            sortedGenres.forEach { genre ->
                                val booksInGenre = booksByGenre[genre]
                                if (!booksInGenre.isNullOrEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = genre,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp)
                                            ) {
                                                items(booksInGenre, key = { it.isbn }) { myBook ->
                                                    val formattedDate = myBook.completedDate?.let {
                                                        val sdf = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
                                                        sdf.format(Date(it))
                                                    } ?: "날짜 정보 없음"

                                                    Column {
                                                        BookCardItem(
                                                            book = Book(
                                                                title = myBook.title,
                                                                author = myBook.author,
                                                                cover = myBook.cover,
                                                                isbn = myBook.isbn,
                                                                itemPage = myBook.totalPages,
                                                                categoryName = myBook.categoryName
                                                            ),
                                                            onClick = {
                                                                expandedBookIsbn = if (expandedBookIsbn == myBook.isbn) {
                                                                    null
                                                                } else {
                                                                    myBook.isbn
                                                                }
                                                            }
                                                        )

                                                        AnimatedVisibility(
                                                            visible = expandedBookIsbn == myBook.isbn,
                                                            enter = expandVertically(animationSpec = tween(300)),
                                                            exit = shrinkVertically(animationSpec = tween(300))
                                                        ) {
                                                            Text(
                                                                text = "완독: $formattedDate",
                                                                modifier = Modifier
                                                                    .padding(top = 8.dp, start = 4.dp),
                                                                fontSize = 12.sp,
                                                                color = Color.Gray
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is ProfileUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ProfileUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "오류가 발생했습니다: ${state.message}",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
