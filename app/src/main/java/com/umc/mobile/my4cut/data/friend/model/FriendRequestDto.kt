package com.umc.mobile.my4cut.data.friend.model

data class FriendRequestDto(
    val requestId: Long,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?
)