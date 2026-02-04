package com.umc.mobile.my4cut.data.friend.model

data class FriendDto(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val isFavorite: Boolean
)