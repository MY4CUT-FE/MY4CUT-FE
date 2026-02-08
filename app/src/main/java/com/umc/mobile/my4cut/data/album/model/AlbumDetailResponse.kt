package com.umc.mobile.my4cut.data.album.model

data class AlbumDetailResponse(
    val id: Int,
    val name: String,
    val photos: List<PhotoResponse>,
    val createdAt: String
)
