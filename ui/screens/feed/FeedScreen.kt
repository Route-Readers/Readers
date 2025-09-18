import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FeedScreen() {
    val mockFeedItems = remember { getMockFeedData() }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FriendStoriesRow(mockFeedItems.map { it.user }.distinctBy { it.id })
        }
        
        items(mockFeedItems) { feedItem ->
            FeedItemCard(feedItem)
        }
    }
}

@Composable
fun FriendStoriesRow(friends: List<User>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(friends) { friend ->
            FriendStoryItem(friend)
        }
    }
}

@Composable
fun FriendStoryItem(user: User) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            
            if (user.isCurrentlyReading) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ReadingGreen)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = user.name,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FeedItemCard(feedItem: FeedItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = feedItem.user.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = feedItem.book.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = feedItem.book.author,
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (feedItem.isCompleted) {
                        Text(
                            text = "완독 완료! 🎉",
                            fontSize = 14.sp,
                            color = CompletedGreen,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "${feedItem.progressPercent}% 진행중",
                            fontSize = 14.sp,
                            color = ProgressBlue
                        )
                        
                        LinearProgressIndicator(
                            progress = feedItem.progressPercent / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ProgressBlue
                        )
                    }
                }
            }
        }
    }
}

private fun getMockFeedData(): List<FeedItem> {
    return listOf(
        FeedItem(
            user = User("1", "김독서", isCurrentlyReading = true),
            book = Book("1", "해리포터와 마법사의 돌", "J.K. 롤링"),
            currentPage = 150,
            progressPercent = 45
        ),
        FeedItem(
            user = User("2", "박책벌레", isCurrentlyReading = false),
            book = Book("2", "1984", "조지 오웰"),
            currentPage = 328,
            progressPercent = 100,
            isCompleted = true
        ),
        FeedItem(
            user = User("3", "이문학", isCurrentlyReading = true),
            book = Book("3", "코스모스", "칼 세이건"),
            currentPage = 89,
            progressPercent = 23
        )
    )
}
