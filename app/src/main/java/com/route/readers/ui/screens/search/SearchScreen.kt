package com.route.readers.ui.screens.search

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.Book
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.data.remote.LibrarySearchResult
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BookViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(), // LibraryViewModel 추가
    myLibraryRepository: MyLibraryRepository = MyLibraryRepository(),
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
                onClick = {
                    selectedTab = 1
                    // 탭 전환 시 검색 결과 및 상태 초기화
                    viewModel.clearSearchResults()
                    libraryViewModel.resetState()
                },
                text = { Text("도서관검색") }
            )
        }

        when (selectedTab) {
            0 -> BookSearchTab(viewModel, myLibraryRepository, firestoreRepository)
            // LibrarySearchTab에 필요한 ViewModel들을 전달
            1 -> LibrarySearchTab(viewModel, libraryViewModel)
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
                        onClick = {
                            // TODO: "읽기 시작" 버튼 클릭 시 동작 구현
                        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySearchTab(
    bookViewModel: BookViewModel,
    libraryViewModel: LibraryViewModel
) {
    var searchText by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    val books by bookViewModel.books.collectAsState()
    val isLoading by bookViewModel.isLoading.collectAsState()
    val libraryState by libraryViewModel.libraryState.collectAsState()

    // 위치 권한 요청 및 처리를 위한 Manager
    val locationManager = rememberLocationManager(
        onLocationGranted = { location ->
            // 위치 정보 획득 성공 시, ViewModel을 통해 도서관 검색 실행
            selectedBook?.let {
                libraryViewModel.searchNearbyLibraries(it.isbn, location.latitude, location.longitude)
            }
        },
        onPermissionDenied = {
            // 권한 거부 시 상태 초기화
            selectedBook = null
            libraryViewModel.resetState()
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 책 검색 UI
        item {
            Column {
                Text(
                    "찾고 있는 책을 검색하여",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "주변 도서관의 소장 정보를 확인해보세요.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("도서관에서 찾을 책 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { bookViewModel.searchBooks(searchText) })
                )
            }
        }

        // 2. 책 검색 결과 또는 도서관 검색 결과 표시
        when (val state = libraryState) {
            is LibraryUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("주변 도서관을 검색 중입니다...", color = TextGray)
                        }
                    }
                }
            }
            is LibraryUiState.Success -> {
                item {
                    Column {
                        Text(
                            "\"${selectedBook?.title}\" 소장 도서관",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.libraries.isEmpty()) {
                            Text("주변에 해당 책을 소장한 도서관이 없습니다.")
                        }
                    }
                }
                items(state.libraries) { result ->
                    LibraryResultCard(
                        libraryName = result.libraryInfo.libName,
                        address = result.libraryInfo.address,
                        distance = result.distance,
                        isLoanAvailable = result.isLoanAvailable
                    )
                }
            }
            is LibraryUiState.Error -> {
                item {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is LibraryUiState.Idle -> {
                // 책 검색 로딩
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                // 책 검색 결과
                items(books) { book ->
                    SimpleBookCard(book = book) {
                        // 책 선택 시, 위치 권한 요청 시작
                        selectedBook = book
                        locationManager()
                    }
                }
            }
        }
    }
}

// LibrarySearchTab에서 사용할 간단한 책 카드
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

// 도서관 검색 결과를 보여주는 카드
@Composable
fun LibraryResultCard(
    libraryName: String,
    address: String,
    distance: Float,
    isLoanAvailable: Boolean
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
                Text(
                    text = if (isLoanAvailable) "✓ 대출 가능" else "✗ 대출 중 또는 불가",
                    color = if (isLoanAvailable) Color(0xFF4CAF50) else DarkRed, // 좀 더 명확한 초록색으로 변경
                    fontWeight = FontWeight.Bold
                )
                Text(
                    // 미터를 킬로미터로 변환하여 소수점 첫째 자리까지 표시
                    text = "${String.format("%.1f", distance / 1000)}km",
                    fontWeight = FontWeight.Bold,
                    color = DarkRed
                )
            }
        }
    }
}

// 이전에 별도 파일로 만들었던 LocationUtils 내용을 여기에 포함시킵니다.
// 만약 별도 파일로 유지하고 싶다면 이 부분은 삭제하고 import 하시면 됩니다.
@SuppressLint("MissingPermission")
@Composable
fun rememberLocationManager(
    onLocationGranted: (Location) -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                hasPermission = true
            } else {
                android.widget.Toast.makeText(context, "위치 권한이 거부되었습니다. 도서관 검색 기능을 사용할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
                onPermissionDenied()
            }
        }
    )

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onLocationGranted(location)
                } else {
                    android.widget.Toast.makeText(context, "위치 정보를 가져오는 데 실패했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                    onPermissionDenied()
                }
            }.addOnFailureListener {
                android.widget.Toast.makeText(context, "위치 서비스 오류가 발생했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                onPermissionDenied()
            }
        }
    }

    return {
        // 권한 요청 시작
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}
