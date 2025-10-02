package com.route.readers.ui.community.used_trade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UsedBookItem(
    title: String,
    author: String,
    seller: String,
    tokens: Int,
    condition: String,
    rating: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 책 이미지 (임시)
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.Gray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                author,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    condition,
                    fontSize = 12.sp,
                    color = Color.Green
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "★ $rating",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800)
                )
            }
            Text(
                "지금 새 책을 읽고있습니다",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    seller,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$tokens 토큰",
                    fontSize = 14.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }
            Button(
                onClick = { /* 구매하기 */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("구매하기", fontSize = 12.sp)
            }
        }
    }
}
