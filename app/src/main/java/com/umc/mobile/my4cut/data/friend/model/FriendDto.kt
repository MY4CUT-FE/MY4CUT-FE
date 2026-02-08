package com.umc.mobile.my4cut.data.friend.model

data class FriendDto(
    val friendId: Long,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val isFavorite: Boolean
)