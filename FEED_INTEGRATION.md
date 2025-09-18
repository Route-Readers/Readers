# Feed 화면 통합 가이드

## 구현 완료된 파일들
```
data/model/
├── User.kt           # 사용자 데이터 모델
├── Book.kt           # 책 데이터 모델  
└── FeedItem.kt       # 피드 아이템 데이터 모델

ui/screens/feed/
├── FeedScreen.kt     # 메인 피드 화면 컴포저블
└── FeedViewModel.kt  # 피드 데이터 관리 뷰모델

ui/theme/
└── Color.kt          # 테마 색상 정의
```

## 메인 액티비티에서 사용하는 방법

```kotlin
// MainActivity.kt 또는 Navigation에서
import com.route.readers.ui.screens.feed.FeedScreen

@Composable
fun MainScreen() {
    // ... 네비게이션 설정
    FeedScreen() // 피드 화면 호출
}
```

## BottomNavBar와 연결
기존 BottomNavBar.kt의 "피드" 텍스트를 클릭했을 때 FeedScreen()을 호출하도록 설정하면 됩니다.

## 주요 기능
✅ 친구 스토리 (인스타그램 스타일)  
✅ 현재 읽고 있는 친구 초록색 점 표시  
✅ 독서 진행률 퍼센트 표시  
✅ 프로그레스 바로 시각적 진행률 표시  
✅ 완독 완료 상태 표시  

## 다음 단계
- 실제 API 연동
- 이미지 로딩 (Coil 등)
- 클릭 이벤트 처리
- 새로고침 기능
