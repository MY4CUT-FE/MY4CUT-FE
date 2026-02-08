package com.umc.mobile.my4cut.data.notification.model

data class NotificationDto(
    val id: Long,
    val type: String,
    val msg: String,
    val isRead: Boolean,
    val createdAt: String
)