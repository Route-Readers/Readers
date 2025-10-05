package com.route.readers.ui.screens.search

import android.util.Log
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BookViewModel = viewModel(),
    libraryRepository: MyLibraryRepository = MyLibraryRepository(),
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
            0 -> BookSearchTab(viewModel, libraryRepository, firestoreRepository)
            1 -> LibrarySearchTab()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchTab(
    viewModel: BookViewModel,
    libraryRepository: MyLibraryRepository,
    firestoreRepository: FirestoreRepository
) {
    var searchText by remember { mutableStateOf("") }
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()

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
                            try {
                                if (searchText.isNotBlank() && searchText.trim().isNotEmpty()) {
                                    viewModel.searchBooks(searchText.trim())
                                }
                            } catch (e: Exception) {
                                Log.e("SearchScreen", "Search button error: ${e.message}", e)
                            }
                        },
                        enabled = searchText.isNotBlank() && !isLoading
                    ) {
                        Text("검색")
                    }
                }
            )
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

        // 로딩 상태
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
                        Text("검색 중...", color = TextGray)
                    }
                }
            }
        }

        // 검색 결과
        if (books.isNotEmpty()) {
            item {
                Text(
                    "검색 결과 (${books.size}권)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkRed
                )
            }

            items(books) { book ->
                var isInLibrary by remember { mutableStateOf(false) }
                
                // 책이 서재에 있는지 확인
                LaunchedEffect(book.isbn) {
                    isInLibrary = libraryRepository.isBookInLibrary(book.isbn)
                }
                
                BookSearchResultCard(
                    book = book,
                    onAddToLibrary = { bookToAdd ->
                        scope.launch {
                            try {
                                val success = libraryRepository.addBookToLibrary(bookToAdd)
                                if (success) {
                                    isInLibrary = true
                                    Log.d("SearchScreen", "Book added successfully: ${bookToAdd.title}")
                                } else {
                                    Log.e("SearchScreen", "Failed to add book: ${bookToAdd.title}")
                                }
                            } catch (e: Exception) {
                                Log.e("SearchScreen", "Error adding book: ${e.message}", e)
                            }
                        }
                    },
                    isInLibrary = isInLibrary
                )
            }
        }

        // 초기 상태일 때 신간 도서 표시
        if (books.isEmpty() && searchText.isBlank() && !isLoading && errorMessage == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "신간 도서",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkRed
                            )
                            TextButton(
                                onClick = { viewModel.getNewBooks() }
                            ) {
                                Text("불러오기")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "최신 출간 도서를 확인해보세요",
                            color = TextGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookSearchResultCard(
    book: Book,
    onAddToLibrary: (Book) -> Unit,
    isInLibrary: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            // 책 표지 (실제 이미지 또는 플레이스홀더)
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

            // 책 정보
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
                if (!book.categoryName.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.categoryName,
                        fontSize = 12.sp,
                        color = DarkRed
                    )
                }
                if (!book.itemPage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${book.itemPage}페이지",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 버튼들
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isInLibrary) {
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = ReadingGreen),
                            modifier = Modifier.height(32.dp),
                            enabled = false
                        ) {
                            Text("서재에 있음", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { onAddToLibrary(book) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("서재 추가", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            // TODO: 책 읽기 시작 로직
                        },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("읽기 시작", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LibrarySearchTab() {
    var searchText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("지역명으로 검색 (예: 강남구)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🗺️",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "지도 영역",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkRed
                        )
                        Text(
                            "(지도 API 연동 예정)",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "주변 서점/도서관",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(3) { index ->
                            LibraryItem("서점/도서관 ${index + 1}", "${index + 1}km")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryItem(name: String, distance: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CreamBackground, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                "서울시 강남구",
                color = TextGray,
                fontSize = 12.sp
            )
        }
        Text(
            distance,
            color = DarkRed,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
