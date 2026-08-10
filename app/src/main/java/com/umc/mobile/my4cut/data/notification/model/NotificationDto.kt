package com.umc.mobile.my4cut.data.notification.model

data class NotificationDto(
    val notificationId: Long,
    val type: String,
    val message: String?,
    val isRead: Boolean,
    val referenceId: Long?,
    val mediaId: Long?,        // 댓글/미디어 알림에서 해당 사진 ID
    val senderId: Long?,
    val senderNickname: String?,
    val senderProfileImageUrl: String?,
    val workspaceId: Long?,
    val workspaceName: String?,
    val createdAt: String?
)
