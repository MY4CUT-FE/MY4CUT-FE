package com.umc.mobile.my4cut.data.auth.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
    // 필요하다면 userId 등 추가
)