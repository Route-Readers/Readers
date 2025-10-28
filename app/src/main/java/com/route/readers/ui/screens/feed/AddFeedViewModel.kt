package com.route.readers.ui.screens.add_feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.Book
import com.route.readers.data.remote.BookRepository
import com.route.readers.ui.screens.feed.FeedItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AddFeedUiState {
    data object Idle : AddFeedUiState()
    data object Loading : AddFeedUiState()
    data object Success : AddFeedUiState()
    data class Error(val message: String) : AddFeedUiState()
}

class AddFeedViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val bookRepository = BookRepository()
    private var searchJob: Job? = null

    var uiState by mutableStateOf<AddFeedUiState>(AddFeedUiState.Idle)
        private set

    var searchText by mutableStateOf("")
        private set
    var searchedBooks by mutableStateOf<List<Book>>(emptyList())
        private set
    var selectedBook by mutableStateOf<Book?>(null)
        private set
    var isSearching by mutableStateOf(false)
        private set

    var reviewText by mutableStateOf("")
    var rating by mutableStateOf(0)

    fun onSearchTextChanged(text: String) {
        searchText = text
        if (text.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(500L)
                isSearching = true
                searchedBooks = try {
                    bookRepository.getBookSearch(query = text, maxResults = 10)
                } catch (e: Exception) {
                    emptyList()
                } finally {
                    isSearching = false
                }
            }
        } else {
            searchedBooks = emptyList()
        }
    }

    fun onBookSelected(book: Book) {
        selectedBook = book
        searchedBooks = emptyList()
        searchText = book.title
    }

    fun clearSelectedBook() {
        selectedBook = null
        searchText = ""
        reviewText = ""
        rating = 0
    }

    fun submitFeed() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            uiState = AddFeedUiState.Error("로그인이 필요합니다.")
            return
        }

        val bookToSave = selectedBook
        if (bookToSave == null) {
            uiState = AddFeedUiState.Error("리뷰를 작성할 책을 선택해주세요.")
            return
        }
        if (reviewText.isBlank()) {
            uiState = AddFeedUiState.Error("리뷰 내용을 입력해주세요.")
            return
        }
        if (rating == 0) {
            uiState = AddFeedUiState.Error("별점을 매겨주세요.")
            return
        }

        uiState = AddFeedUiState.Loading

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val currentUserNickname = userDoc.getString("nickname") ?: "익명"

                val feedRef = db.collection("feeds").document()

                val newFeed = FeedItem.BookReview(
                    id = feedRef.id,
                    authorId = currentUserId,
                    userName = currentUserNickname,
                    book = bookToSave,
                    review = reviewText,
                    rating = rating,
                    timestamp = com.google.firebase.Timestamp.now(),
                    likeCount = 0,
                    commentCount = 0,
                    likedBy = emptyList(),
                    bookmarkedBy = emptyList()
                )

                feedRef.set(newFeed).await()
                uiState = AddFeedUiState.Success
            } catch (e: Exception) {
                uiState = AddFeedUiState.Error("피드 저장에 실패했습니다: ${e.message}")
            }
        }
    }
}
