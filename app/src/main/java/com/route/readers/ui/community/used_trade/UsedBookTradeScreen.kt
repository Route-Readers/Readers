package com.route.readers.ui.community.used_trade

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

import androidx.compose.material.icons.filled.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsedBookTradeScreen(
    viewModel: UsedBookTradeViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit,
    onNavigateToChatList: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<com.route.readers.data.model.UsedBook?>(null) }
    var showEditDialog by remember { mutableStateOf<com.route.readers.data.model.UsedBook?>(null) }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "토큰으로 중고책 거래",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "친구들이 읽은 책을 토큰으로 구매해보세요!",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                if (uiState.books.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "등록된 중고책이 없습니다",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(uiState.books) { book ->
                        UsedBookCard(
                            book = book,
                            isMyBook = book.sellerId == currentUserId,
                            onDeleteClick = { showDeleteDialog = book },
                            onEditClick = { showEditDialog = book },
                            onBuyClick = { },
                            onClick = { onNavigateToDetail(book.id) }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onNavigateToChatList,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                Icon(Icons.Default.Chat, "채팅 목록")
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, "책 등록")
            }
        }
    }

    if (showAddDialog) {
        AddUsedBookDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, author, condition, price, description, cover, _ ->
                viewModel.addBook(title, author, condition, price, description, cover)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { book ->
        AddUsedBookDialog(
            bookToEdit = book,
            onDismiss = { showEditDialog = null },
            onConfirm = { title, author, condition, price, description, cover, bookId ->
                if (bookId != null) {
                    viewModel.updateBook(bookId, title, author, condition, price, description, cover)
                }
                showEditDialog = null
            }
        )
    }

    showDeleteDialog?.let { book ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("삭제 확인") },
            text = { Text("'${book.bookTitle}' 책을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBook(book.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun UsedBookCard(
    book: com.route.readers.data.model.UsedBook,
    isMyBook: Boolean,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onBuyClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AsyncImage(
                model = book.bookCover,
                contentDescription = null,
                modifier = Modifier
                    .width(60.dp)
                    .height(80.dp),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.bookTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = book.bookAuthor,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "판매자: ${book.sellerName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "상태: ${book.condition}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${book.price} 토큰",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA000)
                )
            }
            if (isMyBook) {
                Column {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "수정")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUsedBookDialog(
    bookToEdit: com.route.readers.data.model.UsedBook? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String, String?, String?) -> Unit
) {
    var bookKeyword by remember { mutableStateOf(bookToEdit?.bookTitle ?: "") }
    var selectedBook by remember { mutableStateOf<com.route.readers.data.model.Book?>(
        bookToEdit?.let {
            com.route.readers.data.model.Book(
                title = it.bookTitle,
                author = it.bookAuthor,
                cover = it.bookCover ?: "",
                description = "",
                isbn = "",
                publisher = "",
                pubDate = ""
            )
        }
    ) }
    var searchResults by remember { mutableStateOf<List<com.route.readers.data.model.Book>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    
    var condition by remember { mutableStateOf(bookToEdit?.condition ?: "상급") }
    var price by remember { mutableStateOf(bookToEdit?.price?.toString() ?: "") }
    var description by remember { mutableStateOf(bookToEdit?.description ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val bookRepository = remember { com.route.readers.data.remote.BookRepository() }

    suspend fun searchBooks(query: String, page: Int, append: Boolean = false) {
        if (query.length < 2) {
            searchResults = emptyList()
            showDropdown = false
            return
        }
        
        isSearching = true
        try {
            val results = bookRepository.getBookSearch(query, page = page, maxResults = 20)
            if (append) {
                searchResults = searchResults + results
            } else {
                searchResults = results
            }
            hasMore = results.size == 20
            showDropdown = searchResults.isNotEmpty()
        } catch (e: Exception) {
            if (!append) {
                searchResults = emptyList()
                showDropdown = false
            }
        }
        isSearching = false
    }

    LaunchedEffect(bookKeyword) {
        currentPage = 1
        hasMore = true
        kotlinx.coroutines.delay(500)
        searchBooks(bookKeyword, 1, append = false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bookToEdit == null) "중고책 등록" else "중고책 수정") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = bookKeyword,
                        onValueChange = {
                            bookKeyword = it
                            if (it != selectedBook?.title) {
                                selectedBook = null
                            }
                        },
                        label = { Text("책 검색") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )

                    if (showDropdown && searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            LazyColumn {
                                items(searchResults) { book ->
                                    BookSearchItem(
                                        book = book,
                                        onClick = {
                                            selectedBook = book
                                            bookKeyword = book.title
                                            showDropdown = false
                                        }
                                    )
                                }
                                
                                if (hasMore && !isSearching) {
                                    item {
                                        LaunchedEffect(Unit) {
                                            currentPage++
                                            searchBooks(bookKeyword, currentPage, append = true)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                selectedBook?.let { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = book.cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Text(
                                    text = book.author,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = book.publisher ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("상태") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("상급", "중급", "하급").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    condition = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                    label = { Text("가격 (토큰)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명 (선택)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedBook?.let { book ->
                        if (price.isNotBlank()) {
                            onConfirm(book.title, book.author, condition, price.toInt(), description, book.cover, bookToEdit?.id)
                        }
                    }
                },
                enabled = selectedBook != null && price.isNotBlank()
            ) {
                Text(if (bookToEdit == null) "등록" else "수정")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun BookSearchItem(
    book: com.route.readers.data.model.Book,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = book.cover,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
