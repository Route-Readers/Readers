package com.route.readers.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.BookClub
import kotlinx.coroutines.tasks.await

object BookClubInitializer {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun initializeBookClubs() {
        try {
            // 기존 북클럽이 있는지 확인
            val existingClubs = firestore.collection("bookClubs").get().await()
            if (existingClubs.documents.isNotEmpty()) {
                return // 이미 데이터가 있으면 초기화하지 않음
            }
            
            val initialBookClubs = listOf(
                BookClub(
                    name = "소설 애호가들",
                    description = "한국 소설을 함께 읽고 토론하는 모임입니다. 매주 한 권씩 읽고 감상을 나눕니다.",
                    currentBook = "82년생 김지영",
                    currentBookAuthor = "조남주",
                    nextMeetingDate = "2024.11.15",
                    memberCount = 0,
                    members = emptyList(),
                    createdBy = "system",
                    bookTitle = "82년생 김지영"
                ),
                BookClub(
                    name = "자기계발 독서회",
                    description = "성장을 위한 자기계발서 독서 모임입니다. 실천 가능한 내용들을 함께 공유해요.",
                    currentBook = "아토믹 해빗",
                    currentBookAuthor = "제임스 클리어",
                    nextMeetingDate = "2024.11.20",
                    memberCount = 0,
                    members = emptyList(),
                    createdBy = "system",
                    bookTitle = "아토믹 해빗"
                ),
                BookClub(
                    name = "SF 마니아",
                    description = "SF 소설과 과학 도서를 읽는 모임입니다. 상상력을 자극하는 책들을 함께 읽어요.",
                    currentBook = "삼체",
                    currentBookAuthor = "류츠신",
                    nextMeetingDate = "2024.11.18",
                    memberCount = 0,
                    members = emptyList(),
                    createdBy = "system",
                    bookTitle = "삼체"
                ),
                BookClub(
                    name = "경제/경영 스터디",
                    description = "경제와 경영 관련 도서를 읽고 토론하는 모임입니다.",
                    currentBook = "부의 추월차선",
                    currentBookAuthor = "엠제이 드마코",
                    nextMeetingDate = "2024.11.22",
                    memberCount = 0,
                    members = emptyList(),
                    createdBy = "system",
                    bookTitle = "부의 추월차선"
                ),
                BookClub(
                    name = "철학 카페",
                    description = "철학 도서를 통해 삶의 의미를 탐구하는 모임입니다.",
                    currentBook = "사피엔스",
                    currentBookAuthor = "유발 하라리",
                    nextMeetingDate = "2024.11.25",
                    memberCount = 0,
                    members = emptyList(),
                    createdBy = "system",
                    bookTitle = "사피엔스"
                )
            )
            
            // Firestore에 북클럽 데이터 추가
            initialBookClubs.forEach { bookClub ->
                firestore.collection("bookClubs").add(bookClub).await()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
