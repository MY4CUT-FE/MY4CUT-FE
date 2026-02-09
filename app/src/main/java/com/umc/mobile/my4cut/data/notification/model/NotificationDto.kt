package com.umc.mobile.my4cut.data.notification.model

data class NotificationDto(
    val notificationId: Long,
    val type: String,
    val message: String?,
    val isRead: Boolean,
    val referenceId: Long?,
    val senderId: Long?,
    val senderNickname: String?,
    val senderProfileImageUrl: String?,
    val workspaceId: Long?,
    val workspaceName: String?,
    val createdAt: String?
)