package com.route.readers.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.data.model.Book
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookListScreen(
    onBack: () -> Unit,
    // ProfileViewModel을 공유하여 현재 상태의 myLibraryBooks 리스트를 가져옵니다.
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    // MyBook 객체를 Book 객체로 변환합니다.
                    val readBooks = state.myLibraryBooks.map { myBook ->
                        Book(
                            title = myBook.title,
                            author = myBook.author,
                            cover = myBook.cover,
                            isbn = myBook.isbn
                        )
                    }

                    if (readBooks.isEmpty()) {
                        // TODO: 읽은 책이 없을 때 보여줄 화면을 여기에 구현하세요. (예: Text("아직 읽은 책이 없어요."))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(readBooks) { book ->
                                // BookCardItem을 재사용하여 책 정보를 표시합니다.
                                BookCardItem(
                                    book = book,
                                    onClick = {
                                        // TODO: 각 책을 클릭했을 때의 동작을 여기에 구현하세요. (예: 책 상세 페이지로 이동)
                                    }
                                )
                            }
                        }
                    }
                }
                // 로딩 중이나 에러 상태일 때의 UI 처리
                is ProfileUiState.Loading -> { /* TODO: 로딩 인디케이터 표시 */ }
                is ProfileUiState.Error -> { /* TODO: 에러 메시지 표시 */ }
            }
        }
    }
}
