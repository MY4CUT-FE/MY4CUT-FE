package com.umc.mobile.my4cut.data.user.model

data class UserMeResponse(
    val userId: Int,
    val email: String,
    val nickname: String,
    val friendCode: String,
    val profileImageUrl: String?,
    val loginType: String,
    val thisMonthDay4CutCount: Int
)
