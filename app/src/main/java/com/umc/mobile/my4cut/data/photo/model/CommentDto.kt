package com.umc.mobile.my4cut.data.photo.model

data class CommentDto(
    val id: Long,
    val content: String,
    val writerNickname: String,
    val createdAt: Long
)