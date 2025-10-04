package com.route.readers.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.remote.AddFriendResult
import com.route.readers.data.remote.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Friend(
    val id: String = "",
    val name: String,
    val currentBook: String,
    val isOnline: Boolean,
    val lastActive: String
)

data class CommunityUiState(
    val friends: List<Friend> = emptyList(),
    val challengeProgress: Float = 0.67f,
    val challengeParticipants: Int = 156,
    val addFriendMessage: String? = null,
    val friendToDelete: Friend? = null
) {
    val displayedFriends: List<Friend> = friends.take(5)
    val hasMoreFriends: Boolean = friends.size > 5
}

class CommunityViewModel : ViewModel() {
    private val friendsRepository = FriendsRepository()
    
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            friendsRepository.friends.collect { friends ->
                _uiState.value = _uiState.value.copy(friends = friends)
            }
        }
        loadFriends()
    }
    
    private fun loadFriends() {
        viewModelScope.launch {
            friendsRepository.loadFriends()
        }
    }
    
    fun addFriend(friendName: String) {
        viewModelScope.launch {
            val result = friendsRepository.addFriend(friendName)
            val message = when (result) {
                is AddFriendResult.Success -> "친구가 추가되었습니다!"
                is AddFriendResult.UserNotFound -> "존재하지 않는 사용자입니다."
                is AddFriendResult.AlreadyFriend -> "이미 친구로 추가된 사용자입니다."
                is AddFriendResult.Error -> "오류가 발생했습니다: ${result.message}"
            }
            _uiState.value = _uiState.value.copy(addFriendMessage = message)
        }
    }
    
    fun showDeleteConfirmation(friend: Friend) {
        _uiState.value = _uiState.value.copy(friendToDelete = friend)
    }
    
    fun confirmDeleteFriend() {
        _uiState.value.friendToDelete?.let { friend ->
            viewModelScope.launch {
                friendsRepository.removeFriend(friend.id)
            }
        }
        _uiState.value = _uiState.value.copy(friendToDelete = null)
    }
    
    fun cancelDeleteFriend() {
        _uiState.value = _uiState.value.copy(friendToDelete = null)
    }
    
    fun clearAddFriendMessage() {
        _uiState.value = _uiState.value.copy(addFriendMessage = null)
    }
}
