package com.umc.mobile.my4cut.data.album.model

data class AlbumResponse(
    val id: Int,
    val name: String,
    val photoCount: Int,
    val coverImageUrl: String?,
    val createdAt: String
)