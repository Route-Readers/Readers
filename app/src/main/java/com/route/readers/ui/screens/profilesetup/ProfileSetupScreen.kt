package com.route.readers.ui.screens.profilesetup

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class SelectableItem(val name: String)

val readingGenres = listOf(
    SelectableItem("소설"), SelectableItem("로맨스"), SelectableItem("판타지"),
    SelectableItem("여행"), SelectableItem("자기계발"), SelectableItem("예술"),
    SelectableItem("스릴러"), SelectableItem("역사"), SelectableItem("과학"), SelectableItem("에세이")
)

val readingStyles = listOf(
    SelectableItem("빠르게 읽기"), SelectableItem("천천히 음미하기"), SelectableItem("다양한 분야 읽기"),
    SelectableItem("한 분야 깊게 파기"), SelectableItem("가볍게 즐기기"), SelectableItem("토론하며 읽기")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var nickname by remember { mutableStateOf("") }
    var isNicknameChecked by remember { mutableStateOf(false) }
    var isNicknameAvailable by remember { mutableStateOf(false) }
    var nicknameCheckMessage by remember { mutableStateOf<String?>(null) }
    var selectedGenres by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedStyles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }

    val primaryRed = Color(0xFFC0392B)

    fun checkNicknameAvailability() {
        if (nickname.length !in 2..12) {
            nicknameCheckMessage = "닉네임은 2~12자 사이로 입력해주세요."
            return
        }
        isLoading = true
        firestore.collection("users").whereEqualTo("nickname", nickname).get()
            .addOnSuccessListener { documents ->
                isLoading = false
                isNicknameChecked = true
                if (documents.isEmpty) {
                    isNicknameAvailable = true
                    nicknameCheckMessage = "사용 가능한 닉네임입니다."
                } else {
                    isNicknameAvailable = false
                    nicknameCheckMessage = "이미 사용 중인 닉네임입니다."
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                nicknameCheckMessage = "오류 발생: ${e.message}"
            }
    }

    fun saveProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isNicknameAvailable) {
            Toast.makeText(context, "닉네임 중복 확인을 완료해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedGenres.isEmpty()) {
            Toast.makeText(context, "선호 장르를 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        val userProfile = hashMapOf(
            "uid" to currentUser.uid,
            "nickname" to nickname,
            "email" to currentUser.email,
            "reading_genres" to selectedGenres.toList(),
            "reading_styles" to selectedStyles.toList()
        )

        firestore.collection("users").document(currentUser.uid).set(userProfile)
            .addOnSuccessListener {
                isLoading = false
                Toast.makeText(context, "프로필 설정이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                onSetupComplete()
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "저장에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "프로필 아이콘",
                modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, primaryRed, CircleShape).padding(16.dp),
                tint = primaryRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("닉네임 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("다른 독서 애호가들과 소통할 때 사용할 닉네임을 정해주세요.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Section("나만의 닉네임") {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        if (it.length <= 12) {
                            nickname = it
                            isNicknameChecked = false
                            isNicknameAvailable = false
                            nicknameCheckMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("닉네임 (2~12자)") },
                    singleLine = true,
                    trailingIcon = { Text("${nickname.length}/12", modifier = Modifier.padding(end = 8.dp), color = Color.Gray) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                nicknameCheckMessage?.let {
                    val messageColor = if (isNicknameAvailable) Color(0xFF27AE60) else MaterialTheme.colorScheme.error
                    Text(it, color = messageColor, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { checkNicknameAvailability() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryRed.copy(alpha = 0.8f))
                ) {
                    Text("중복 확인")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Section("선호하는 장르 (다중 선택 가능)") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    readingGenres.forEach { item ->
                        SelectableChip(
                            text = item.name,
                            isSelected = selectedGenres.contains(item.name),
                            onClick = {
                                selectedGenres = if (selectedGenres.contains(item.name)) selectedGenres - item.name else selectedGenres + item.name
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Section("독서 스타일 (다중 선택 가능)") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    readingStyles.forEach { item ->
                        SelectableChip(
                            text = item.name,
                            isSelected = selectedStyles.contains(item.name),
                            onClick = {
                                selectedStyles = if (selectedStyles.contains(item.name)) selectedStyles - item.name else selectedStyles + item.name
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            Button(
                onClick = { saveProfile() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryRed),
                enabled = !isLoading && isNicknameAvailable
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("설정 완료하고 시작하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
fun SelectableChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFFC0392B) else Color.LightGray
    val containerColor = if (isSelected) Color(0xFFC0392B).copy(alpha = 0.1f) else Color.Transparent

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(borderColor))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = borderColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text, color = if (isSelected) borderColor else Color.Black, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileSetupScreenPreview() {
    ProfileSetupScreen(onSetupComplete = {})
}
