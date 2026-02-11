package com.umc.mobile.my4cut.data.photo.model

data class CommentDto(
    val id: Long,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val content: String,
    val createdAt: String
)