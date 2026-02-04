package com.umc.mobile.my4cut.data.auth.model

import com.google.gson.annotations.SerializedName

// 일반 로그인 요청
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)