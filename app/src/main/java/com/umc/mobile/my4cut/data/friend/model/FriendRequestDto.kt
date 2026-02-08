package com.umc.mobile.my4cut.data.friend.model

data class FriendRequestDto(
    val requestId: Long,
    val requesterId: Long,
    val requesterNickname: String,
    val createdAt: String
)