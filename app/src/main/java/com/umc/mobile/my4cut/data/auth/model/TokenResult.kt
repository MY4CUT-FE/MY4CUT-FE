package com.umc.mobile.my4cut.data.auth.model

// 로그인, 리프레시, 카카오 로그인 성공 시 공통적으로 받는 data 내부 객체
data class TokenResult(
    val userId: Int,
    val accessToken: String,
    val refreshToken: String
)