package com.route.readers.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.AdminAction
import com.route.readers.data.model.AdminLog
import com.route.readers.data.model.Report
import com.route.readers.data.model.User
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun isAdmin(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val doc = db.collection("users").document(uid).get().await()
        val role = doc.getString("role") ?: "user"
        return role == "admin" || role == "superadmin"
    }

    suspend fun getCurrentUserRole(): String {
        val uid = auth.currentUser?.uid ?: return "user"
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("role") ?: "user"
    }

    // 신고 제출 + 로그 기록
    suspend fun submitReport(report: Report): Result<Unit> {
        return try {
            val docRef = db.collection("reports").document()
            val reportWithId = report.copy(id = docRef.id)
            docRef.set(reportWithId).await()
            
            // 신고 접수 로그 기록
            logAdminAction(AdminAction.NEW_REPORT, report.targetOwnerId, report.targetType, "[${report.reporterNickname}]이(가) 신고 | 사유: ${report.reason}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getTargetNickname(userId: String, targetType: String): String {
        return try {
            if (targetType == "feed") {
                val feedDoc = db.collection("feeds").document(userId).get().await()
                val authorId = feedDoc.getString("authorId") ?: userId
                val userDoc = db.collection("users").document(authorId).get().await()
                userDoc.getString("nickname") ?: "알 수 없음"
            } else {
                val userDoc = db.collection("users").document(userId).get().await()
                userDoc.getString("nickname") ?: "알 수 없음"
            }
        } catch (e: Exception) {
            "알 수 없음"
        }
    }

    // 대기 중인 신고 목록
    suspend fun getPendingReports(): List<Report> {
        return try {
            db.collection("reports")
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(Report::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 모든 신고 목록
    suspend fun getAllReports(): List<Report> {
        return try {
            db.collection("reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get().await()
                .toObjects(Report::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 유저 경고
    suspend fun warnUser(userId: String, reason: String): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(userId)
            val userDoc = userRef.get().await()
            val currentWarnings = userDoc.getLong("warnings")?.toInt() ?: 0
            val newWarnings = currentWarnings + 1

            val updates = mutableMapOf<String, Any>(
                "warnings" to newWarnings
            )

            // 3회 경고 시 자동 7일 정지
            if (newWarnings >= 3) {
                updates["isBanned"] = true
                updates["banReason"] = "경고 3회 누적"
                updates["banExpiry"] = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
            }

            userRef.update(updates).await()
            logAdminAction(AdminAction.WARN_USER, userId, "user", "경고 사유: $reason (${newWarnings}회)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 유저 정지
    suspend fun banUser(userId: String, reason: String, durationDays: Int? = null): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>(
                "isBanned" to true,
                "banReason" to reason,
                "banExpiry" to if (durationDays != null) {
                    System.currentTimeMillis() + (durationDays * 24 * 60 * 60 * 1000L)
                } else null
            )

            db.collection("users").document(userId).update(updates).await()
            val duration = if (durationDays != null) "${durationDays}일" else "영구"
            logAdminAction(AdminAction.BAN_USER, userId, "user", "정지 사유: $reason ($duration)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 유저 정지 해제
    suspend fun unbanUser(userId: String): Result<Unit> {
        return try {
            db.collection("users").document(userId).update(
                mapOf(
                    "isBanned" to false,
                    "banReason" to null,
                    "banExpiry" to null
                )
            ).await()
            logAdminAction(AdminAction.UNBAN_USER, userId, "user", "정지 해제")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 피드 삭제
    suspend fun deleteFeed(feedId: String, reason: String): Result<Unit> {
        return try {
            db.collection("feeds").document(feedId).delete().await()
            logAdminAction(AdminAction.DELETE_FEED, feedId, "feed", "삭제 사유: $reason")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 신고 처리
    suspend fun resolveReport(reportId: String, actionTaken: String): Result<Unit> {
        return try {
            val adminId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to "resolved",
                    "resolvedBy" to adminId,
                    "resolvedAt" to System.currentTimeMillis(),
                    "actionTaken" to actionTaken
                )
            ).await()
            logAdminAction(AdminAction.RESOLVE_REPORT, reportId, "report", "조치: $actionTaken")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 신고 기각
    suspend fun dismissReport(reportId: String): Result<Unit> {
        return try {
            val adminId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to "dismissed",
                    "resolvedBy" to adminId,
                    "resolvedAt" to System.currentTimeMillis(),
                    "actionTaken" to "none"
                )
            ).await()
            logAdminAction(AdminAction.DISMISS_REPORT, reportId, "report", "신고 기각")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 유저 검색
    suspend fun searchUsers(query: String): List<User> {
        return try {
            val byNickname = db.collection("users")
                .whereGreaterThanOrEqualTo("nickname", query)
                .whereLessThanOrEqualTo("nickname", query + "\uf8ff")
                .limit(20)
                .get().await()
                .toObjects(User::class.java)

            val byEmail = db.collection("users")
                .whereEqualTo("email", query)
                .get().await()
                .toObjects(User::class.java)

            (byNickname + byEmail).distinctBy { it.uid }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 유저 정보 조회
    suspend fun getUser(userId: String): User? {
        return try {
            db.collection("users").document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // 관리자 활동 로그 기록
    private suspend fun logAdminAction(action: String, targetId: String, targetType: String, details: String) {
        try {
            val adminId = auth.currentUser?.uid ?: return
            val adminDoc = db.collection("users").document(adminId).get().await()
            val adminNickname = adminDoc.getString("nickname") ?: "Unknown"

            val log = AdminLog(
                adminId = adminId,
                adminNickname = adminNickname,
                action = action,
                targetId = targetId,
                targetType = targetType,
                details = details
            )
            val docRef = db.collection("admin_logs").document()
            db.collection("admin_logs").document(docRef.id).set(log.copy(id = docRef.id)).await()
        } catch (e: Exception) {
            // 로그 실패는 무시
        }
    }

    // 관리자 활동 로그 조회
    suspend fun getAdminLogs(limit: Int = 50): List<AdminLog> {
        return try {
            db.collection("admin_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
                .toObjects(AdminLog::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 정지 상태 확인 (로그인 시 사용)
    suspend fun checkBanStatus(userId: String): BanStatus {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            val isBanned = doc.getBoolean("isBanned") ?: false
            if (!isBanned) return BanStatus.NotBanned

            val banExpiry = doc.getLong("banExpiry")
            val banReason = doc.getString("banReason") ?: "정책 위반"

            // 정지 기간 만료 확인
            if (banExpiry != null && System.currentTimeMillis() > banExpiry) {
                // 자동 정지 해제
                doc.reference.update(
                    mapOf("isBanned" to false, "banReason" to null, "banExpiry" to null)
                ).await()
                return BanStatus.NotBanned
            }

            BanStatus.Banned(banReason, banExpiry)
        } catch (e: Exception) {
            BanStatus.NotBanned
        }
    }
}

sealed class BanStatus {
    object NotBanned : BanStatus()
    data class Banned(val reason: String, val expiry: Long?) : BanStatus()
}
