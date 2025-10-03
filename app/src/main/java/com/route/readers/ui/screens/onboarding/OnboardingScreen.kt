package com.route.readers.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.R
import com.route.readers.ui.theme.ReadersTheme

@Composable
fun OnboardingScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val primaryRed = Color(0xFFC0392B)
    val lightGray = Color(0xFFBDC3C7)
    val backgroundColor = Color.White

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(primaryRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.mipmap.readerslogo),
                    contentDescription = "App Main Logo",
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Readers",
                color = primaryRed,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "독서의 시작",
                color = lightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavigateToSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryRed)
            ) {
                Text("시작하기", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text("계정이 이미 있습니다", fontSize = 16.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    ReadersTheme {
        OnboardingScreen(onNavigateToSignUp = {}, onNavigateToLogin = {})
    }
}

