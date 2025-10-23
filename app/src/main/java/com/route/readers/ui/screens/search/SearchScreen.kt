package com.route.readers.ui.screens.search

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.route.readers.R
import com.route.readers.data.model.Book
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BookViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    myLibraryRepository: MyLibraryRepository = MyLibraryRepository(),
    firestoreRepository: FirestoreRepository = FirestoreRepository()
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
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
                onClick = {
                    selectedTab = 1
                    viewModel.clearSearchResults()
                    libraryViewModel.resetState()
                },
                text = { Text("도서관검색") }
            )
        }

        when (selectedTab) {
            0 -> BookSearchTab(viewModel, myLibraryRepository, firestoreRepository)
            1 -> LibrarySearchTab(bookViewModel = viewModel, libraryViewModel = libraryViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchTab(
    viewModel: BookViewModel,
    myLibraryRepository: MyLibraryRepository,
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
                isInLibrary = myLibraryRepository.isBookInLibrary(book.isbn)
            }

            BookSearchResultCard(
                book = book,
                isInLibrary = isInLibrary,
                onAddToLibrary = {
                    scope.launch {
                        val success = myLibraryRepository.addBookToLibrary(it)
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
                model = book.getHighQualityImageUrl().ifEmpty { R.mipmap.readerslogo },
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

                Row(
                    modifier = Modifier.wrapContentWidth(align = Alignment.Start),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInLibrary) {
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.LightGray,
                                contentColor = Color.DarkGray
                            ),
                            modifier = Modifier.height(32.dp),
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("서재에 있음", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { onAddToLibrary(book) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("서재 추가", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DarkRed
                        ),
                        border = BorderStroke(1.dp, DarkRed),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
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

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun LibrarySearchTab(
    bookViewModel: BookViewModel,
    libraryViewModel: LibraryViewModel
) {
    var searchText by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    val books by bookViewModel.books.collectAsState()
    val isLoadingBooks by bookViewModel.isLoading.collectAsState()
    val libraryState by libraryViewModel.libraryState.collectAsState()
    val context = LocalContext.current
    // ▼▼▼ fusedLocationClient를 remember 안으로 이동 ▼▼▼
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    fun requestLocationSearch(book: Book? = null) {
        libraryViewModel.startLoading()
        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                if (book != null) {
                    libraryViewModel.searchNearbyLibrariesWithBook(
                        isbn = book.isbn,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    libraryViewModel.searchNearbyLibraries(location.latitude, location.longitude)
                }
            } else {
                libraryViewModel.notifyLocationError()
            }
        }.addOnFailureListener {
            libraryViewModel.notifyLocationError()
        }
    }

    LaunchedEffect(permissionState.allPermissionsGranted, libraryState) {
        if (libraryState is LibraryUiState.Idle) {
            if (permissionState.allPermissionsGranted) {
                requestLocationSearch()
            } else {
                libraryViewModel.notifyPermissionError()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("찾고 있는 책을 검색하여", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("주변 도서관의 소장 정보를 확인해보세요.", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("도서관에서 찾을 책 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        selectedBook = null
                        libraryViewModel.resetState()
                        bookViewModel.searchBooks(searchText)
                    })
                )
            }
        }

        when (val state = libraryState) {
            is LibraryUiState.Loading -> {
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (selectedBook != null) "책 소장 정보를 검색 중입니다..." else "주변 도서관을 검색 중입니다...",
                                color = TextGray
                            )
                        }
                    }
                }
            }
            is LibraryUiState.Success -> {
                item {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedBook != null) "\"${selectedBook?.title}\" 소장 도서관" else "내 주변 도서관",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (state.libraries.isEmpty()) {
                    item {
                        Text(
                            if (selectedBook != null) "주변에 해당 책을 소장한 도서관이 없습니다." else "주변에 검색된 도서관이 없습니다."
                        )
                    }
                } else {
                    items(state.libraries, key = { it.libraryInfo.libCode }) { result ->
                        LibraryResultCard(
                            libraryName = result.libraryInfo.libName,
                            address = result.libraryInfo.address,
                            distance = result.distance,
                            isLoanAvailable = result.isLoanAvailable,
                            showLoanStatus = selectedBook != null
                        )
                    }
                }
            }
            is LibraryUiState.Error -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (!permissionState.allPermissionsGranted) {
                                permissionState.launchMultiplePermissionRequest()
                            } else {
                                requestLocationSearch(selectedBook)
                            }
                        }) {
                            Text("다시 시도")
                        }
                    }
                }
            }
            is LibraryUiState.Idle -> {
                if (isLoadingBooks) {
                    item {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (books.isNotEmpty()) {
                    items(books, key = { it.isbn }) { book ->
                        SimpleBookCard(book = book) {
                            selectedBook = book
                            if (permissionState.allPermissionsGranted) {
                                requestLocationSearch(book)
                            } else {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleBookCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.getHighQualityImageUrl().ifEmpty { R.mipmap.readerslogo },
                contentDescription = "책 표지",
                modifier = Modifier
                    .size(40.dp, 60.dp)
                    .background(ReadingGreen, RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.readerslogo)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(book.author, color = TextGray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun LibraryResultCard(
    libraryName: String,
    address: String,
    distance: Float,
    isLoanAvailable: Boolean,
    showLoanStatus: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(libraryName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(address, color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showLoanStatus) {
                    Text(
                        text = if (isLoanAvailable) "✓ 대출 가능" else "✗ 대출 중 또는 불가",
                        color = if (isLoanAvailable) Color(0xFF4CAF50) else DarkRed,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Text(
                    text = "${String.format("%.1f", distance / 1000)}km",
                    fontWeight = FontWeight.Bold,
                    color = DarkRed
                )
            }
        }
    }
}

