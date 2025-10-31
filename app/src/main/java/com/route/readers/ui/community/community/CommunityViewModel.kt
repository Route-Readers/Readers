package com.route.readers.ui.community.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.route.readers.data.model.Challenge // 새로 만든 Challenge 모델 import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Friend(
    val name: String,
    val currentBook: String,
    val isOnline: Boolean,
    val lastActive: String
)

// ▼▼▼ CommunityUiState를 새로운 데이터 구조에 맞게 수정 ▼▼▼
data class CommunityUiState(
    val friends: List<Friend> = emptyList(),
    val challenges: List<Challenge> = emptyList(), // 챌린지 리스트 추가
    val isLoadingChallenges: Boolean = false // 로딩 상태 추가
)
// ▲▲▲ 수정 완료 ▲▲▲

class CommunityViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ▼▼▼ _uiState 초기값을 비어있는 상태로 변경 ▼▼▼
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    init {
        // ViewModel이 생성될 때 친구 목록과 챌린지 목록을 불러옵니다.
        loadFriends()
        loadWeeklyChallenges()
    }

    // 친구 목록을 불러오는 함수 (현재는 하드코딩 유지)
    private fun loadFriends() {
        val friendList = listOf(
            Friend("김지연", "최근 본 책: 아토믹 해빗", true, "온라인"),
            Friend("박민수", "최근 본 책: 사피엔스", false, "30분 전"),
            Friend("이서현", "최근 본 책: 해리 포터와 마법사의 돌", false, "1시간 전")
        )
        _uiState.update { it.copy(friends = friendList) }
    }

    // ▼▼▼ 챌린지 관련 함수들을 ViewModel에 추가 ▼▼▼

    /**
     * Firestore의 'challenges' 컬렉션에서 주간 챌린지 목록을 불러옵니다.
     */
    fun loadWeeklyChallenges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChallenges = true) }
            try {
                // 'challenges' 컬렉션에서 문서를 가져옵니다.
                // 실제 운영 시에는 주간 챌린지를 필터링하는 로직을 추가할 수 있습니다.
                // (예: .whereEqualTo("isWeekly", true))
                val snapshot = db.collection("challenges").get().await()
                val challengeList = snapshot.documents.mapNotNull { doc ->
                    // Firestore 문서를 Challenge 객체로 변환하고, 문서 ID를 Challenge의 id 필드에 할당
                    doc.toObject<Challenge>()?.copy(id = doc.id)
                }
                _uiState.update {
                    it.copy(challenges = challengeList, isLoadingChallenges = false)
                }
            } catch (e: Exception) {
                // 오류 발생 시 로딩 상태를 해제
                _uiState.update { it.copy(isLoadingChallenges = false) }
                // TODO: 사용자에게 오류 메시지를 보여주는 로직 추가
            }
        }
    }

    /**
     * 사용자가 특정 챌린지에 참여합니다.
     * @param challengeId 참여할 챌린지의 문서 ID
     */
    fun joinChallenge(challengeId: String) {
        val userId = auth.currentUser?.uid ?: return // 로그인한 사용자가 없으면 함수 종료

        viewModelScope.launch {
            try {
                // 'challenges' 컬렉션에서 해당 챌린지 문서의 'participants' 필드에 사용자 UID를 추가
                db.collection("challenges").document(challengeId).update(
                    "participants", FieldValue.arrayUnion(userId)
                ).await()

                // 참여 성공 후, UI를 최신 상태로 갱신하기 위해 챌린지 목록을 다시 불러옴
                loadWeeklyChallenges()
            } catch (e: Exception) {
                // TODO: 사용자에게 오류 메시지를 보여주는 로직 추가
            }
        }
    }
    // ▲▲▲ 챌린지 관련 함수 추가 완료 ▲▲▲
}
