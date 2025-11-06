package com.route.readers.ui.screens.search

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.route.readers.data.remote.MyLibraryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    bookViewModel: BookViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel()
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
                onClick = {
                    selectedTab = 0
                    libraryViewModel.resetState()
                },
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
            0 -> BookSearchTab(bookViewModel, libraryViewModel)
            1 -> LibrarySearchTab(libraryViewModel = libraryViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchTab(
    bookViewModel: BookViewModel,
    libraryViewModel: LibraryViewModel
) {
    val currentQuery by bookViewModel.currentQuery.collectAsState()
    var searchText by remember { mutableStateOf(currentQuery) }
    val books by bookViewModel.books.collectAsState()
    val isLoading by bookViewModel.isLoading.collectAsState()
    val isLoadingMore by bookViewModel.isLoadingMore.collectAsState()
    val errorMessage by bookViewModel.errorMessage.collectAsState()
    val hasMoreResults by bookViewModel.hasMoreResults.collectAsState()

    val libraryState by libraryViewModel.libraryState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(libraryState) {
        val successState = libraryState as? LibraryUiState.Success
        successState?.infoMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            libraryViewModel.clearInfoMessage()
        }

        val errorState = libraryState as? LibraryUiState.Error
        errorState?.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(currentQuery) {
        searchText = currentQuery
    }

    fun performSearch() {
        if (searchText.isNotBlank()) {
            bookViewModel.performSearch(searchText.trim(), isNewSearch = true)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
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

        if (books.isNotEmpty()) {
            if (currentQuery.isNotEmpty()) {
                item {
                    Text(
                        "\"$currentQuery\" 검색 결과 (${books.size}권)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(books, key = { book -> book.isbn13 ?: book.isbn ?: book.title }) { book ->
                BookSearchItem(
                    book = book,
                    libraryViewModel = libraryViewModel,
                    bookViewModel = bookViewModel
                )
            }
        }

        if (hasMoreResults && books.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = { bookViewModel.loadMoreBooks() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("더보기")
                        }
                    }
                }
            }
        }

        if (books.isEmpty() && !isLoading && errorMessage == null) {
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
                            TextButton(onClick = { bookViewModel.getNewBooks() }) {
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
private fun BookSearchItem(
    book: Book,
    libraryViewModel: LibraryViewModel,
    bookViewModel: BookViewModel
) {
    val myLibraryRepository = remember { MyLibraryRepository() }
    val coroutineScope = rememberCoroutineScope()

    val isInLibraryState = produceState(initialValue = false, key1 = book.isbn13, key2 = book.isbn) {
        val isbn = book.isbn13?.takeIf { it.isNotBlank() } ?: book.isbn?.takeIf { it.isNotBlank() }
        if (isbn != null) {
            value = myLibraryRepository.isBookInLibrary(isbn)
        }
    }

    BookSearchResultCard(
        book = book,
        isInLibrary = isInLibraryState.value,
        onAddToLibrary = {
            coroutineScope.launch {
                (isInLibraryState as? MutableState<Boolean>)?.value = true
                libraryViewModel.addBookToLibrary(it)
            }
        },
        onToggleFavorite = { bookViewModel.onToggleFavorite(it) }
    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .size(80.dp, 120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
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
    libraryViewModel: LibraryViewModel
) {
    var searchText by remember { mutableStateOf("") }
    val libraryState by libraryViewModel.libraryState.collectAsState()

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    fun performSearch() {
        if (searchText.isBlank()) return

        permissionState.launchMultiplePermissionRequest()

        if (permissionState.allPermissionsGranted) {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        libraryViewModel.startLibrarySearch(searchText, location, context)
                    } else {
                        libraryViewModel.notifyLocationError()
                    }
                }.addOnFailureListener {
                    libraryViewModel.notifyLocationError()
                }
        } else {
            libraryViewModel.notifyLocationError()
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
                    onValueChange = { searchText = it },
                    placeholder = { Text("도서관에서 찾을 책 검색", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                    )
                )
            }
        }

        when (val currentLibraryState = libraryState) {
            is LibraryUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (currentLibraryState.libraryCode == null) "'${currentLibraryState.query ?: searchText}' 소장 도서관을 검색중입니다..."
                                else "대출 정보를 확인 중입니다...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            is LibraryUiState.Success -> {
                if (currentLibraryState.query.isNotEmpty()) {
                    item {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "『${currentLibraryState.query}』 소장 도서관",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                if (currentLibraryState.libraries.isEmpty() && currentLibraryState.query.isNotEmpty()) {
                    item {
                        Text("주변에 해당 책을 소장한 도서관이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(currentLibraryState.libraries, key = { it.libraryInfo.libCode }) { library ->
                        val isSelected = currentLibraryState.selectedLibrary?.libraryInfo?.libCode == library.libraryInfo.libCode
                        LibraryResultCard(
                            libraryName = library.libraryInfo.libName,
                            address = library.libraryInfo.address,
                            distance = library.distance,
                            onClick = {
                                if (!isSelected) {
                                    libraryViewModel.checkBookAvailabilityInLibrary(library, currentLibraryState.books)
                                } else {
                                    libraryViewModel.resetState()
                                }
                            },
                            isSelected = isSelected,
                            availability = if (isSelected) currentLibraryState.availability else null,
                            isLoading = (libraryState as? LibraryUiState.Loading)?.libraryCode == library.libraryInfo.libCode,
                            books = currentLibraryState.books
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
                        Button(onClick = { performSearch() }) {
                            Text("다시 시도")
                        }
                    }
                }
            }
            is LibraryUiState.Idle -> {
                if (searchText.isBlank()) {
                    item {
                        Text("책 제목을 검색하면 소장하고 있는 주변 도서관 목록이 나타납니다.", modifier = Modifier.padding(top=32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryResultCard(
    libraryName: String,
    address: String,
    distance: Float,
    onClick: () -> Unit,
    isSelected: Boolean,
    availability: Map<String, Boolean>?,
    isLoading: Boolean,
    books: List<Book>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(libraryName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${String.format("%.1f", distance / 1000)}km",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(address, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
            } else if (isSelected && availability != null) {
                Spacer(modifier = Modifier.height(16.dp))
                if (availability.isNotEmpty()) {
                    val availableBooksCount = availability.count { it.value }

                    if (availableBooksCount > 0) {
                        Text("대출 가능 도서 (${availableBooksCount}종)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Text("모든 버전이 대출 중이거나 소장하고 있지 않습니다.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    books.forEach { book ->
                        book.isbn13?.let { isbn ->
                            if (availability.containsKey(isbn)) {
                                val isAvailable = availability[isbn] ?: false
                                val loanStatusColor = if (isAvailable) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAvailable) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = loanStatusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = book.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("검색된 책을 소장하고 있지 않습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}
