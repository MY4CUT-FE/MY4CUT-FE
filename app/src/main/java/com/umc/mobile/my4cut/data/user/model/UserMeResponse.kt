package com.umc.mobile.my4cut.data.user.model

data class  UserMeResponse(
    val userId: Int,
    val email: String,
    val nickname: String,
    val friendCode: String,
    val profileImageFileKey: String?,
    val profileImageViewUrl: String?,
    val loginType: String,
    val thisMonthDay4CutCount: Int
)