package com.umc.mobile.my4cut.ui.notification

data class NotificationData(
    val id: Long,             // 서버에서 내려온 알림 또는 요청 ID
    val type: String,         // 알림 타입 (FRIEND_REQUEST, WORKSPACE_INVITE 등)
    val iconResId: Int,       // 아이콘 리소스 ID (R.drawable.xxx)
    val category: String,     // 카테고리 (초대, 친구, 댓글)
    val content: String,      // 알림 내용
    val time: String,         // 시간 (13분 전, 2일 전)
    val hasButtons: Boolean   // 수락/거절 버튼 표시 여부 (true면 보임)
)