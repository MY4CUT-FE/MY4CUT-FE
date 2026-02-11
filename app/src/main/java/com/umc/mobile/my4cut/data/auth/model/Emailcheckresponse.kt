package com.umc.mobile.my4cut.data.auth.model

import com.google.gson.annotations.SerializedName

/**
 * 이메일 중복 체크 응답
 */
data class EmailCheckResponse(
    @SerializedName("email") val email: String,
    @SerializedName("duplicated") val duplicated: Boolean  // true: 중복됨, false: 사용 가능
)