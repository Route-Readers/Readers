package com.route.readers.ui.screens.search

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.Book
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
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentQuery by viewModel.currentQuery.collectAsState()
    val hasMoreResults by viewModel.hasMoreResults.collectAsState()
    val scope = rememberCoroutineScope()

    fun performSearch() {
        if (searchText.isNotBlank()) {
            viewModel.searchBooks(searchText.trim())
        }
    }

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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                trailingIcon = {
                    Button(
                        onClick = { performSearch() },
                        enabled = searchText.isNotBlank() && !isLoading
                    ) {
                        Text("검색")
                    }
                }
            )
        }

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

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("검색 중...", color = TextGray)
                    }
                }
            }
        }

        if (books.isNotEmpty() && currentQuery.isNotEmpty()) {
            item {
                Text(
                    "\"$currentQuery\" 검색 결과 (${books.size}권)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkRed
                )
            }
        }

        items(books, key = { it.isbn }) { book ->
            var isInLibrary by remember(book.isbn) { mutableStateOf(false) }

            LaunchedEffect(book.isbn) {
                isInLibrary = libraryRepository.isBookInLibrary(book.isbn)
            }

            BookSearchResultCard(
                book = book,
                isInLibrary = isInLibrary,
                onAddToLibrary = {
                    scope.launch {
                        val success = libraryRepository.addBookToLibrary(it)
                        if (success) {
                            isInLibrary = true
                        }
                    }
                },
                onToggleFavorite = { viewModel.onToggleFavorite(it) }
            )
        }

        if (hasMoreResults && currentQuery.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkRed)
                    } else {
                        Button(
                            onClick = { viewModel.loadMoreBooks() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("더보기")
                        }
                    }
                }
            }
        }

        if (books.isEmpty() && searchText.isBlank() && !isLoading && errorMessage == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                            TextButton(onClick = { viewModel.getNewBooks() }) {
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
    isInLibrary: Boolean,
    onAddToLibrary: (Book) -> Unit,
    onToggleFavorite: (Book) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = book.cover.ifEmpty { R.mipmap.readerslogo },
                contentDescription = "책 표지",
                modifier = Modifier
                    .size(80.dp, 100.dp)
                    .background(ReadingGreen, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.readerslogo)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
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
                val pageCount = book.extractPageCount()
                if (pageCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${pageCount}페이지",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ▼▼▼ 이 Row의 Modifier를 수정했습니다 ▼▼▼
                Row(
                    // width를 제한하지 않고, 내용물(자식)의 크기에 맞게 너비가 결정되도록 합니다.
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInLibrary) {
                        // "서재에 있음" 버튼
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.LightGray,
                                contentColor = Color.DarkGray
                            ),
                            modifier = Modifier.height(32.dp),
                            enabled = false,
                            // 버튼의 내부 패딩을 조절하여 텍스트에 맞게 크기가 조절되도록 합니다.
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("서재에 있음", fontSize = 12.sp)
                        }
                    } else {
                        // "서재 추가" 버튼
                        Button(
                            onClick = { onAddToLibrary(book) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("서재 추가", fontSize = 12.sp)
                        }
                    }

                    // "읽기" 버튼
                    OutlinedButton(
                        onClick = {
                            // TODO: "읽기" 버튼 클릭 시 동작 구현
                        },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DarkRed
                        ),
                        border = BorderStroke(1.dp, DarkRed),
                        // 버튼의 내부 패딩을 조절하여 텍스트에 맞게 크기가 조절되도록 합니다.
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        // "서재에 있음" 상태일 때는 "읽기", 아닐 때는 "읽기 시작"
                        Text(if (isInLibrary) "읽기" else "읽기 시작", fontSize = 12.sp)
                    }
                }
            }

            IconButton(onClick = { onToggleFavorite(book) }) {
                Icon(
                    imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "관심 도서",
                    tint = if (book.isFavorite) DarkRed else Color.Gray
                )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️", fontSize = 48.sp)
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "주변 서점/도서관",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
