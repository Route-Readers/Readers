package com.route.readers.data.model

object ReportReason {
    const val SPAM = "스팸"
    const val HARASSMENT = "괴롭힘"
    const val INAPPROPRIATE_CONTENT = "부적절한 콘텐츠"
    const val HATE_SPEECH = "혐오 발언"
    const val VIOLENCE = "폭력적 내용"
    const val COPYRIGHT = "저작권 침해"
    const val FAKE_INFO = "허위 정보"
    const val OTHER = "기타"
    
    val all = listOf(
        SPAM,
        HARASSMENT,
        INAPPROPRIATE_CONTENT,
        HATE_SPEECH,
        VIOLENCE,
        COPYRIGHT,
        FAKE_INFO,
        OTHER
    )
}
