package com.route.readers.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.User
import kotlinx.coroutines.tasks.await
import android.util.Log // Add this import

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private fun getUsersCollection() = db.collection("users")

    suspend fun getUser(userId: String): User? {
        return try {
            getUsersCollection().document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // `likeAlarmEnabled` 필드가 Firestore 문서에 존재하는지 확인하고, 없으면 true로 설정합니다.
    suspend fun ensureLikeAlarmEnabled(userId: String) {
        try {
            val userDocRef = getUsersCollection().document(userId)
            val userSnapshot = userDocRef.get().await()

            if (!userSnapshot.contains("likeAlarmEnabled")) {
                // 필드가 없으면 기본값인 true로 설정
                userDocRef.update("likeAlarmEnabled", true).await()
                Log.d("UserRepository", "likeAlarmEnabled 필드를 $userId 사용자 문서에 추가했습니다.")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "likeAlarmEnabled 필드 확인 및 업데이트 중 오류 발생: $e")
        }
    }
}
