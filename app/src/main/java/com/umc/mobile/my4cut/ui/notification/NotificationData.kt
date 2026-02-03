package com.umc.mobile.my4cut.data

// 알림 데이터 클래스
data class NotificationData(
    val iconResId: Int,       // 아이콘 리소스 ID (R.drawable.xxx)
    val category: String,     // 카테고리 (초대, 친구, 댓글)
    val content: String,      // 알림 내용
    val time: String,         // 시간 (13분 전, 2일 전)
    val hasButtons: Boolean   // 수락/거절 버튼 표시 여부 (true면 보임)
)