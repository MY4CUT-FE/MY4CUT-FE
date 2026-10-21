package com.umc.mobile.my4cut.data.auth.model

import com.google.gson.annotations.SerializedName

/**
 * 이메일 인증코드 검증 응답 (회원가입 / 비밀번호 재설정 공통)
 */
data class EmailVerifyResult(
    @SerializedName("verificationToken") val verificationToken: String
)
