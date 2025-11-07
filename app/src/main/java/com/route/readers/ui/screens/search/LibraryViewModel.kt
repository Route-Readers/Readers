package com.route.readers.ui.screens.search

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.BookRepository
import com.route.readers.data.remote.LibraryRepository
import com.route.readers.data.remote.LibrarySearchResult
import com.route.readers.data.remote.MyLibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class LibraryUiState {
    object Idle : LibraryUiState()
    data class Loading(val query: String? = null, val libraryCode: String? = null) : LibraryUiState()
    data class Success(
        val query: String,
        val libraries: List<LibrarySearchResult>,
        val books: List<Book> = emptyList(),
        val selectedLibrary: LibrarySearchResult? = null,
        val availability: Map<String, Boolean>? = null,
        val isAddingBook: Boolean = false,
        val infoMessage: String? = null
    ) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}

class LibraryViewModel(
    private val libraryRepository: LibraryRepository = LibraryRepository(),
    private val bookRepository: BookRepository = BookRepository(),
    private val myLibraryRepository: MyLibraryRepository = MyLibraryRepository()
) : ViewModel() {

    private val _libraryState = MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)
    val libraryState: StateFlow<LibraryUiState> = _libraryState.asStateFlow()

    fun startLibrarySearch(query: String, location: Location, context: Context) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _libraryState.value = LibraryUiState.Idle
                return@launch
            }
            _libraryState.value = LibraryUiState.Loading(query)

            try {
                val allRelatedBooks = bookRepository.getBookSearch(query = query, page = 1, maxResults = 50)
                if (allRelatedBooks.isEmpty()) {
                    _libraryState.value = LibraryUiState.Success(query, emptyList(), emptyList())
                    return@launch
                }

                val targetBooks = allRelatedBooks.filter { it.title.contains(query, ignoreCase = true) }
                if (targetBooks.isEmpty()) {
                    _libraryState.value = LibraryUiState.Success(query, emptyList(), allRelatedBooks.take(10))
                    return@launch
                }

                val targetIsbns = targetBooks.mapNotNull { it.isbn13?.takeIf { it.isNotBlank() } }.distinct()
                if (targetIsbns.isEmpty()) {
                    _libraryState.value = LibraryUiState.Success(query, emptyList(), targetBooks)
                    return@launch
                }

                val libraries = libraryRepository.getNearbyLibrariesWithBooks(
                    context = context,
                    isbns = targetIsbns,
                    userLatitude = location.latitude,
                    userLongitude = location.longitude
                )
                _libraryState.value = LibraryUiState.Success(query, libraries, targetBooks)

            } catch (e: Exception) {
                Log.e("LibraryViewModel", "startLibrarySearch 중 오류 발생", e)
                _libraryState.value = LibraryUiState.Error("검색 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    fun addBookToLibrary(bookFromSearch: Book) {
        val isbn = bookFromSearch.isbn13?.takeIf { it.isNotBlank() } ?: bookFromSearch.isbn?.takeIf { it.isNotBlank() }
        if (isbn == null) {
            _libraryState.value = LibraryUiState.Error("ISBN 정보가 없어 추가할 수 없는 책입니다.")
            return
        }

        viewModelScope.launch {
            val currentState = _libraryState.value
            val successState = if (currentState is LibraryUiState.Success) {
                currentState
            } else {
                LibraryUiState.Success(query = bookFromSearch.title, libraries = emptyList(), books = listOf(bookFromSearch))
            }

            _libraryState.value = successState.copy(isAddingBook = true, infoMessage = null)

            try {
                val detailedBook = bookRepository.getBookDetail(isbn)
                if (detailedBook != null) {
                    val newMyBook = MyBook(
                        id = detailedBook.isbn13?.takeIf { it.isNotBlank() } ?: detailedBook.isbn ?: "",
                        title = detailedBook.title,
                        author = detailedBook.author,
                        isbn = detailedBook.isbn13?.takeIf { it.isNotBlank() } ?: detailedBook.isbn ?: "",
                        cover = detailedBook.cover,
                        totalPages = detailedBook.extractPageCount(),
                        currentPage = 0,
                        isCompleted = false,
                        addedDate = System.currentTimeMillis()
                    )

                    val success = myLibraryRepository.addBookToLibrary(newMyBook)
                    val message = if (success) {
                        "'${newMyBook.title}'을(를) 서재에 추가했습니다."
                    } else {
                        "이미 서재에 있는 책입니다."
                    }
                    _libraryState.update {
                        if (it is LibraryUiState.Success) {
                            it.copy(isAddingBook = false, infoMessage = message)
                        } else {
                            it
                        }
                    }
                } else {
                    _libraryState.update {
                        if (it is LibraryUiState.Success) {
                            it.copy(isAddingBook = false, infoMessage = "책의 상세 정보를 가져오는데 실패했습니다.")
                        } else {
                            it
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "addBookToLibrary 중 오류 발생", e)
                _libraryState.value = LibraryUiState.Error("책 추가 중 오류가 발생했습니다.")
            }
        }
    }

    fun clearInfoMessage() {
        val currentState = _libraryState.value
        if (currentState is LibraryUiState.Success) {
            _libraryState.value = currentState.copy(infoMessage = null)
        }
    }

    fun checkBookAvailabilityInLibrary(library: LibrarySearchResult, books: List<Book>) {
        val currentState = _libraryState.value
        if (currentState !is LibraryUiState.Success) return

        viewModelScope.launch {
            _libraryState.value = currentState.copy(selectedLibrary = library, availability = null)
            _libraryState.value = LibraryUiState.Loading(currentState.query, library.libraryInfo.libCode)

            try {
                val isbns = books.mapNotNull { it.isbn13?.takeIf { it.isNotBlank() } }.distinct()
                val availabilityMap = libraryRepository.getBooksAvailability(library.libraryInfo.libCode, isbns)

                val previousState = _libraryState.value
                if (previousState is LibraryUiState.Loading) {
                    _libraryState.value = currentState.copy(
                        selectedLibrary = library,
                        availability = availabilityMap
                    )
                }
            } catch (e: Exception) {
                _libraryState.value = LibraryUiState.Error("대출 정보 확인 중 오류가 발생했습니다.")
            }
        }
    }

    fun notifyLocationError() {
        _libraryState.value = LibraryUiState.Error("위치 정보를 가져올 수 없습니다. 권한을 확인하거나 GPS를 켜주세요.")
    }

    fun resetState() {
        val currentState = _libraryState.value
        if (currentState is LibraryUiState.Success && currentState.selectedLibrary != null) {
            _libraryState.value = currentState.copy(selectedLibrary = null, availability = null)
        } else {
            _libraryState.value = LibraryUiState.Idle
        }
    }
}
