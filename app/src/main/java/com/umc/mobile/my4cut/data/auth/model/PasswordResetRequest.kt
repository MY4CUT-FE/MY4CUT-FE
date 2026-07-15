package com.umc.mobile.my4cut.data.auth.model

import com.google.gson.annotations.SerializedName

data class PasswordResetRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("newPassword") val newPassword: String
)
