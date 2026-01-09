package com.route.readers.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.data.model.Book
import com.route.readers.ui.theme.ReadingGreen

@Composable
fun LibraryBookAvailabilityCard(
    libraryName: String,
    address: String,
    distance: Float,
    onClick: () -> Unit,
    isSelected: Boolean,
    availability: Map<String, Boolean>?,
    isLoading: Boolean,
    books: List<Book>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = libraryName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format("%.1f", distance / 1000)}km",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = address,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (isSelected && availability != null) {
                Spacer(modifier = Modifier.height(16.dp))
                if (availability.isNotEmpty()) {
                    val availableBooksCount = availability.count { it.value }
                    val totalBooksCount = availability.size

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "소장 도서 현황",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "대출가능 ${availableBooksCount}/${totalBooksCount}권",
                            fontSize = 12.sp,
                            color = if (availableBooksCount > 0) ReadingGreen else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    books.forEach { book ->
                        book.isbn13?.let { isbn ->
                            if (availability.containsKey(isbn)) {
                                val isAvailable = availability[isbn] ?: false
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isAvailable) 
                                            ReadingGreen.copy(alpha = 0.1f) 
                                        else 
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAvailable) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (isAvailable) ReadingGreen else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = book.title,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = book.author,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = if (isAvailable) "대출가능" else "대출중",
                                            fontSize = 11.sp,
                                            color = if (isAvailable) ReadingGreen else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "검색된 책을 소장하고 있지 않습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
