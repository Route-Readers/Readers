@Composable
fun BottomNavBar() {
    // 그냥 네모난 상자에 글씨만 보이게 만듭니다.
    // 나중에 진짜가 완성되면 이 파일의 내용만 교체될 겁니다.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.LightGray),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("피드")
        Text("커뮤니티")
        Text("검색")
        Text("프로필")
    }
}