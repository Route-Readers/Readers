package com.route.readers.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.Report
import com.route.readers.data.model.ReportReason
import com.route.readers.data.remote.AdminRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ReportDialog(
    targetId: String,
    targetType: String, // "user", "feed", "chat", "bookclub_chat"
    targetOwnerId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var otherReason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("신고하기", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("신고 사유를 선택해주세요", fontSize = 14.sp)
                
                ReportReason.all.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason)
                    }
                }

                if (selectedReason == ReportReason.OTHER) {
                    OutlinedTextField(
                        value = otherReason,
                        onValueChange = { otherReason = it },
                        label = { Text("상세 사유") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedReason == null) {
                        Toast.makeText(context, "신고 사유를 선택해주세요", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val result = submitReport(
                            targetId = targetId,
                            targetType = targetType,
                            targetOwnerId = targetOwnerId,
                            reason = selectedReason!!,
                            reasonDetail = if (selectedReason == ReportReason.OTHER) otherReason else null
                        )
                        isSubmitting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "신고가 접수되었습니다", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "신고 접수에 실패했습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = selectedReason != null && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("신고")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private suspend fun submitReport(
    targetId: String,
    targetType: String,
    targetOwnerId: String,
    reason: String,
    reasonDetail: String?
): Result<Unit> {
    return try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(uid).get().await()
        val nickname = userDoc.getString("nickname") ?: "Unknown"

        val report = Report(
            reporterId = uid,
            reporterNickname = nickname,
            targetId = targetId,
            targetType = targetType,
            targetOwnerId = targetOwnerId,
            reason = reason,
            reasonDetail = reasonDetail
        )

        AdminRepository().submitReport(report)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
