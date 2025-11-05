package com.route.readers.ui.screens.search

import android.annotation.SuppressLint
import android.location.Location
import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.route.readers.R
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    bookViewModel: BookViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    myLibraryRepository: MyLibraryRepository = MyLibraryRepository(),
    firestoreRepository: FirestoreRepository = FirestoreRepository()
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
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
                    libraryViewModel.resetState()
                },
                text = { Text("도서관검색") }
            )
        }

        when (selectedTab) {
            0 -> BookSearchTab(bookViewModel, myLibraryRepository, firestoreRepository)
            1 -> LibrarySearchTab(bookViewModel, libraryViewModel)
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
    val currentQuery by viewModel.currentQuery.collectAsState()
    var searchText by remember { mutableStateOf(currentQuery) }
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasMoreResults by viewModel.hasMoreResults.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentQuery) {
        searchText = currentQuery
    }

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
                onValueChange = {
                    searchText = it
                    viewModel.searchBooks(it)
                },
                placeholder = { Text("책 제목으로 검색", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                trailingIcon = {
                    TextButton(
                        onClick = { performSearch() },
                        enabled = searchText.isNotBlank() && !isLoading,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("검색", fontSize = 16.sp)
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("검색 중...", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    color = MaterialTheme.colorScheme.primary
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
                        val totalPages = it.extractPageCount()
                        val myBook = MyBook(
                            id = it.isbn,
                            title = it.title,
                            author = it.author,
                            cover = it.cover,
                            isbn = it.isbn,
                            totalPages = totalPages,
                            categoryName = it.categoryName
                        )
                        val success = myLibraryRepository.addBookToLibrary(myBook)
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = { viewModel.loadMoreBooks() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { viewModel.getNewBooks() }) {
                                Text("불러오기")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "최신 출간 도서를 확인해보세요",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val displayedCategoryName = remember(book.categoryName) {
        val categories = book.categoryName?.split(">")?.map { it.trim() }
        if (categories != null && categories.size > 1) {
            categories[1]
        } else {
            categories?.firstOrNull()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.readerslogo)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!displayedCategoryName.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayedCategoryName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val pageCount = book.extractPageCount()
                if (pageCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${pageCount}페이지",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
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
                    tint = if (book.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun LibrarySearchTab(
    bookViewModel: BookViewModel,
    libraryViewModel: LibraryViewModel
) {
    val currentQuery by bookViewModel.currentQuery.collectAsState()
    var searchText by remember { mutableStateOf(currentQuery) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    val books by bookViewModel.books.collectAsState()
    val isLoadingBooks by bookViewModel.isLoading.collectAsState()
    val libraryState by libraryViewModel.libraryState.collectAsState()

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(currentQuery) {
        searchText = currentQuery
    }

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

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (books.isEmpty() && permissionState.allPermissionsGranted) {
            requestLocationSearch()
        } else if (!permissionState.allPermissionsGranted) {
            libraryViewModel.notifyPermissionError()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("찾고 있는 책을 검색하여", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("주변 도서관의 소장 정보를 확인해보세요.", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        bookViewModel.searchBooks(it)
                    },
                    placeholder = { Text("도서관에서 찾을 책 검색", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        selectedBook = null
                        libraryViewModel.resetState()
                        bookViewModel.searchBooks(searchText)
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }

        val currentLibraryState = libraryState

        if (isLoadingBooks) {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (books.isNotEmpty()) {
            if (currentLibraryState is LibraryUiState.Idle) {
                items(books, key = { "lib-book-${it.isbn}" }) { book ->
                    SimpleBookCard(book = book) {
                        selectedBook = book
                        requestLocationSearch(book)
                    }
                }
            }
        }

        when (currentLibraryState) {
            is LibraryUiState.Loading -> {
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (selectedBook != null) "『${selectedBook?.title}』 소장 정보를 검색 중입니다..." else "주변 도서관을 검색 중입니다...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = if (selectedBook != null) "『${selectedBook?.title}』 소장 도서관" else "내 주변 도서관",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (currentLibraryState.libraries.isEmpty()) {
                    item {
                        Text(
                            if (selectedBook != null) "주변에 해당 책을 소장한 도서관이 없습니다." else "주변에 검색된 도서관이 없습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(currentLibraryState.libraries, key = { it.libraryInfo.libCode }) { result ->
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(currentLibraryState.message, color = MaterialTheme.colorScheme.error)
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
                if (books.isEmpty() && !isLoadingBooks) {
                    item {
                        // Empty state can be added here if needed
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.getHighQualityImageUrl().ifEmpty { R.mipmap.readerslogo },
                contentDescription = "책 표지",
                modifier = Modifier
                    .size(40.dp, 60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.readerslogo)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(libraryName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(address, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showLoanStatus) {
                    val loanStatusColor = if (isLoanAvailable) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    Text(
                        text = if (isLoanAvailable) "✓ 대출 가능" else "✗ 대출 중 또는 불가",
                        color = loanStatusColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Text(
                    text = "${String.format("%.1f", distance / 1000)}km",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}