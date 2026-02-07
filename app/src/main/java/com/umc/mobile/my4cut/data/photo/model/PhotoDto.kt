package com.umc.mobile.my4cut.data.photo.model

data class PhotoDto(
    val id: Long,
    val imageUrl: String,
    val uploaderNickname: String,
    val createdAt: Long
)