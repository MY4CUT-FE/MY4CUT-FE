package com.umc.mobile.my4cut.data.album.model

data class AlbumResponse(
    val id: Int,
    val name: String,
    val mediaCount: Int,
    val coverImageKey: String?,
    val coverImageUrl: String?,
    val createdAt: String
)