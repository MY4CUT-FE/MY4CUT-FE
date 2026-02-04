package com.umc.mobile.my4cut.data.auth.model

import com.google.gson.annotations.SerializedName

// 회원가입 요청
data class SignUpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("nickname") val nickname: String
)