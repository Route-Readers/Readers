package com.route.readers.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.data.remote.BookRepository
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BookViewModel = viewModel(),
    firestoreRepository: FirestoreRepository = FirestoreRepository()
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkRed,
            contentColor = White
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("도서검색") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("도서관검색") }
            )
        }

        when (selectedTab) {
            0 -> BookSearchTab(viewModel, firestoreRepository)
            1 -> LibrarySearchTab()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchTab(
    viewModel: BookViewModel,
    firestoreRepository: FirestoreRepository
) {
    var searchText by remember { mutableStateOf("") }
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bookRepository = remember { BookRepository() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("책 제목, 작가명으로 검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Button(
                        onClick = {
                            if (searchText.isNotBlank()) {
                                viewModel.searchBooks(searchText.trim())
                            }
                        },
                        enabled = searchText.isNotBlank() && !isLoading
                    ) {
                        Text("검색")
                    }
                }
            )
        }

        // 테스트 버튼 추가
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        viewModel.searchBooks("해리포터")
                    },
                    enabled = !isLoading
                ) {
                    Text("해리포터 검색")
                }
                Button(
                    onClick = { viewModel.getNewBooks() },
                    enabled = !isLoading
                ) {
                    Text("신간 불러오기")
                }
            }
        }

        // 로딩 상태 표시
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("검색 중...")
                    }
                }
            }
        }

        // 에러 메시지 표시
        errorMessage?.let { message ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // 검색 결과가 있을 때만 표시
        if (books.isNotEmpty()) {
            items(books) { book ->
                BookSearchResultCard(
                    book = book,
                    onAddToLibrary = { 
                        scope.launch {
                            // 먼저 기본 페이지 수 확인
                            var totalPages = book.getPageCount()
                            
                            // 페이지 정보가 없으면 API로 다시 조회
                            if (totalPages == 0) {
                                bookRepository.getPageInfo(book.isbn)?.let { pageInfo ->
                                    totalPages = pageInfo
                                }
                            }
                            
                            val myBook = MyBook(
                                id = book.isbn,
                                title = book.title,
                                author = book.author,
                                cover = book.cover,
                                isbn = book.isbn,
                                totalPages = totalPages.takeIf { it > 0 } ?: 300
                            )
                            val success = firestoreRepository.addBookToLibrary(myBook)
                            if (success) {
                                Toast.makeText(context, "서재에 추가되었습니다", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "추가 실패. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        } else if (!isLoading && errorMessage == null) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "검색어를 입력하고 검색해보세요",
                        color = TextGray,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BookSearchResultCard(
    book: Book,
    onAddToLibrary: () -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var isAdded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = book.cover.ifEmpty { null },
                contentDescription = "책 표지",
                modifier = Modifier
                    .size(80.dp, 100.dp)
                    .background(ReadingGreen, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.readerslogo),
                placeholder = painterResource(R.mipmap.readerslogo)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    fontSize = 14.sp,
                    color = TextGray
                )
                
                val pageCount = book.getPageCount()
                if (pageCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${pageCount}페이지",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!isAdding && !isAdded) {
                            isAdding = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdded) ReadingGreen else DarkRed
                    ),
                    modifier = Modifier.height(32.dp),
                    enabled = !isAdding && !isAdded
                ) {
                    when {
                        isAdding -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = White,
                                    strokeWidth = 2.dp
                                )
                                Text("추가 중...", fontSize = 12.sp)
                            }
                        }
                        isAdded -> {
                            Text("추가 완료", fontSize = 12.sp)
                        }
                        else -> {
                            Text("서재 추가", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // 추가 로직 처리
    LaunchedEffect(isAdding) {
        if (isAdding) {
            onAddToLibrary() // 실제 서재 추가 실행
            kotlinx.coroutines.delay(1000) // 1초 후
            isAdding = false
            isAdded = true
            kotlinx.coroutines.delay(2000) // 2초 후 원래 상태로
            isAdded = false
        }
    }
}

@Composable
fun LibrarySearchTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("도서관 검색 기능 준비 중")
    }
}
