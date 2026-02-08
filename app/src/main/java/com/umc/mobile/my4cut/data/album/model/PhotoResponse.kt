package com.umc.mobile.my4cut.data.album.model

data class PhotoResponse(
    val id: Int,
    val fileUrl: String,
    val mediaType: String,
    val takenDate: String,
    val isFinal: Boolean,
    val createdAt: String,
    val uploaderNickname: String
)
