package com.route.readers.ui.screens.bookclub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.data.model.Book
import com.route.readers.ui.components.BookClubCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookClubScreen(
    viewModel: BookClubViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("북클럽") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "북클럽 만들기")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.bookClubs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "아직 생성된 북클럽이 없습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.bookClubs) { bookClub ->
                    BookClubCard(
                        bookClub = bookClub,
                        onJoinClick = { viewModel.joinBookClub(bookClub.id) }
                    )
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateBookClubDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, book -> viewModel.createBookClub(name, book) }
        )
    }
}

@Composable
fun CreateBookClubDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Book?) -> Unit
) {
    var clubName by remember { mutableStateOf("") }
    var bookTitle by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var searchResults by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    val bookRepository = remember { com.route.readers.data.remote.BookRepository() }

    LaunchedEffect(bookTitle) {
        if (bookTitle.length >= 2) {
            isSearching = true
            delay(500) // 디바운싱
            try {
                val results = bookRepository.getBookSearch(bookTitle, maxResults = 5)
                searchResults = results
                showDropdown = results.isNotEmpty()
            } catch (e: Exception) {
                searchResults = emptyList()
                showDropdown = false
            }
            isSearching = false
        } else {
            searchResults = emptyList()
            showDropdown = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 북클럽 만들기") },
        text = {
            Column {
                OutlinedTextField(
                    value = clubName,
                    onValueChange = { clubName = it },
                    label = { Text("클럽 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    OutlinedTextField(
                        value = bookTitle,
                        onValueChange = {
                            bookTitle = it
                            if (it != selectedBook?.title) {
                                selectedBook = null
                            }
                        },
                        label = { Text("현재 읽을 책") },
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
                                            bookTitle = book.title
                                            showDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                selectedBook?.let { book ->
                    Spacer(modifier = Modifier.height(16.dp))
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
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = book.author,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(clubName, selectedBook) },
                enabled = clubName.isNotBlank() && selectedBook != null
            ) {
                Text("만들기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun BookSearchItem(
    book: Book,
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
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
