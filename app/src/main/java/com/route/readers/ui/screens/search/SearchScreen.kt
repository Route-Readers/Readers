package com.route.readers.ui.screens.search

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
// Material 3 imports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.route.readers.ui.theme.PrimaryRed
import com.route.readers.ui.theme.ReadingGreen
import com.route.readers.ui.theme.TransparentPrimary
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    bookViewModel: BookViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    onStartReading: (Book) -> Unit = {}
)
{
    var selectedTab by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) // Material 3 sheet state
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SlidingTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                tabs = listOf("도서 검색", "도서관 검색"),
                modifier = Modifier.weight(1f)
            )
            if (selectedTab == 0) { // "도서 검색" 탭일 때만 필터 버튼 표시
                IconButton(onClick = { scope.launch { sheetState.show() } }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter),
                        contentDescription = "검색 필터",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> BookSearchTab(bookViewModel, libraryViewModel, onStartReading)
            1 -> LibrarySearchTab(libraryViewModel = libraryViewModel)
        }
    }

    // ModalBottomSheet를 조건부로 표시
    if (sheetState.isVisible) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { scope.launch { sheetState.hide() } },
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            if (selectedTab == 0) {
                FilterBottomSheetContent(
                    bookViewModel = bookViewModel,
                    onClose = { scope.launch { sheetState.hide() } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookSearchTab(
    bookViewModel: BookViewModel,
    libraryViewModel: LibraryViewModel,
    onStartReading: (Book) -> Unit
)
{
    val currentQuery by bookViewModel.currentQuery.collectAsState()
    var searchText by remember { mutableStateOf(currentQuery) }
    val books by bookViewModel.books.collectAsState()
    val isLoading by bookViewModel.isLoading.collectAsState()
    val isLoadingMore by bookViewModel.isLoadingMore.collectAsState()
    val errorMessage by bookViewModel.errorMessage.collectAsState()
    val hasMoreResults by bookViewModel.hasMoreResults.collectAsState()

    val libraryState by libraryViewModel.libraryState.collectAsState()
    val context = LocalContext.current

    // selectedGenres와 selectedSort 상태를 Composable에서 수집
    val selectedGenresState by bookViewModel.selectedGenres.collectAsState()
    val selectedSortState by bookViewModel.selectedSort.collectAsState()


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

    // `selectedGenresState`와 `selectedSortState`의 변경을 감지하여 검색을 다시 수행하도록 수정
    LaunchedEffect(searchText, selectedGenresState, selectedSortState) {
        // 초기 로드 또는 필터 변경 시 검색을 수행
        // books.isEmpty() 조건은 앱 시작 시 한 번만 검색하도록 합니다.
        // searchText.isNotBlank()는 검색창에 텍스트가 있을 때만 검색이 되도록 합니다.
        // selectedGenresState.isNotEmpty()는 장르가 선택되었을 때 검색이 되도록 합니다.
        // selectedSortState != "정확도순"은 정렬 기준이 기본값이 아닐 때 검색이 되도록 합니다.
        if (books.isEmpty() || searchText.isNotBlank() || selectedGenresState.isNotEmpty() || selectedSortState != "정확도순") {
            bookViewModel.performSearch(
                query = searchText, // 현재 검색창의 텍스트를 사용
                selectedGenres = selectedGenresState,
                sort = selectedSortState,
                isNewSearch = true
            )
        }
    }


    fun performSearch() {
        bookViewModel.performSearch(
            query = searchText.trim(),
            selectedGenres = selectedGenresState,
            sort = selectedSortState,
            isNewSearch = true
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
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
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { performSearch() },
                            enabled = searchText.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .background(
                                    color = if (searchText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "검색",
                                tint = if (searchText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
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
            item {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    val headerText = when {
                        searchText.isNotBlank() -> "\"$searchText\" 검색 결과 (${books.size}권)"
                        selectedGenresState.isNotEmpty() && selectedSortState != "정확도순" ->
                            "${selectedGenresState.joinToString(", ")} 장르, ${selectedSortState} 도서"
                        selectedGenresState.isNotEmpty() ->
                            "${selectedGenresState.joinToString(", ")} 장르 도서"
                        selectedSortState != "정확도순" ->
                            "${selectedSortState} 도서"
                        else ->
                            "따끈따끈한 신간 도서"
                    }
                    Text(
                        headerText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (headerText == "따끈따끈한 신간 도서") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "지금 서점에서 가장 인기 있는 책들을 만나보세요!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            itemsIndexed(
                books,
                key = { _, book ->
                    book.isbn13?.takeIf { it.isNotBlank() } ?: book.isbn ?: book.title
                }
            ) { _, book ->
                BookSearchItem(
                    book = book,
                    libraryViewModel = libraryViewModel,
                    bookViewModel = bookViewModel,
                    onStartReading = onStartReading
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
           // Initial empty state
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheetContent(bookViewModel: BookViewModel, onClose: () -> Unit) {
    val selectedGenres by bookViewModel.selectedGenres.collectAsState()
    val availableGenres = bookViewModel.availableGenres
    val selectedSort by bookViewModel.selectedSort.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Bottom sheet 배경색 적용
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("검색 필터", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "정렬 기준",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bookViewModel.availableSorts.forEach { sort ->
                    FilterChip(
                        selected = selectedSort == sort,
                        onClick = {
                            bookViewModel.onSortSelected(sort)
                            // onClose() // 필터 선택 시 바로 닫을지 여부는 UX에 따라 결정
                        },
                        label = { Text(sort) },
                        leadingIcon = if (selectedSort == sort) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "선택됨",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "장르 필터",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        availableGenres.forEach { (category, genres) ->
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genres.forEach { genre ->
                            FilterChip(
                                selected = selectedGenres.contains(genre),
                                onClick = { bookViewModel.onGenreSelected(genre) },
                                label = { Text(genre) },
                                leadingIcon = if (selectedGenres.contains(genre)) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "선택됨",
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
                        }
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
    bookViewModel: BookViewModel,
    onStartReading: (Book) -> Unit
)
{
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
        onToggleFavorite = { bookViewModel.onToggleFavorite(it) },
        onStartReading = onStartReading
    )
}

@Composable
fun BookSearchResultCard(
    book: Book,
    isInLibrary: Boolean,
    onAddToLibrary: (Book) -> Unit,
    onToggleFavorite: (Book) -> Unit,
    onStartReading: (Book) -> Unit
)
{
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
                    text = "${book.author} · ${book.genre}",
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
                    
                    
                    // 관심 도서 하트 아이콘
                    IconButton(
                        onClick = { onToggleFavorite(book) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "관심 도서",
                            tint = if (book.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 바로읽기 아이콘 (오른쪽 끝) - 서재에 추가하고 읽기 시작
            IconButton(onClick = { 
                onAddToLibrary(book)
                onStartReading(book) 
            }) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "바로읽기",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun LibrarySearchTab(
    libraryViewModel: LibraryViewModel,
    nearbyLibraryViewModel: NearbyLibraryViewModel = viewModel()
)
{
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    val libraryState by libraryViewModel.libraryState.collectAsState()
    val nearbyLibraryState by nearbyLibraryViewModel.uiState.collectAsState()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    LaunchedEffect(Unit) {
        if (permissionState.allPermissionsGranted) {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        nearbyLibraryViewModel.fetchNearbyLibraries(location)
                    }
                }
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    fun performSearch() {
        if (searchText.isBlank()) return

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
                        focusedContainerColor = TransparentPrimary,
                        unfocusedContainerColor = TransparentPrimary
                    )
                )
            }
        }

        if (searchText.isBlank()) {
            item {
                Text(
                    text = "내 주변 도서관",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            when (val state = nearbyLibraryState) {
                is NearbyLibraryUiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is NearbyLibraryUiState.Success -> {
                    val sortedLibraries = state.libraries.sortedBy { it.distance }
                    items(
                        items = sortedLibraries,
                        key = { library -> library.libraryInfo.libCode }
                    ) { library ->
                        LibraryResultCard(
                            libraryName = library.libraryInfo.libName,
                            address = library.libraryInfo.address,
                            distance = library.distance,
                            onClick = { /* Handle click */ },
                            isSelected = false,
                            availability = null,
                            isLoading = false,
                            books = emptyList()
                        )
                    }
                }
                is NearbyLibraryUiState.Error -> {
                    item {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> { // Idle
                }
            }
        } else {
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
                        items(
                            items = currentLibraryState.libraries,
                            key = { library -> library.libraryInfo.libCode }
                        ) { library ->
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
)
{
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
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
                                val loanStatusColor = if (isAvailable) ReadingGreen else MaterialTheme.colorScheme.error
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
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

@Composable
private fun SlidingTabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    modifier: Modifier = Modifier,
)
{
    val density = LocalDensity.current
    var tabPositions by remember { mutableStateOf(List(tabs.size) { Pair(0f, 0f) }) }

    val currentTabPosition = tabPositions.getOrElse(selectedTab) { Pair(0f, 0f) }
    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { currentTabPosition.first.toDp() },
        animationSpec = tween(300),
        label = "indicatorOffset"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { currentTabPosition.second.toDp() },
        animationSpec = tween(300),
        label = "indicatorWidth"
    )

    Box(modifier = modifier.height(36.dp)) {
        // 슬라이딩 배경
        if (indicatorWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(PrimaryRed.copy(alpha = 0.15f))
            )
        }

        // 탭 텍스트들
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onTabSelected(index) }
                        .onGloballyPositioned { coordinates ->
                            val offset = coordinates.positionInParent().x
                            val width = coordinates.size.width.toFloat()
                            if (tabPositions[index] != Pair(offset, width)) {
                                tabPositions = tabPositions.toMutableList().also { 
                                    it[index] = Pair(offset, width)
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) PrimaryRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
