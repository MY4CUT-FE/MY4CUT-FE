package com.umc.mobile.my4cut.data.notification.model

data class RegisterTokenRequestDto(
    val fcmToken: String,
    val device: String = "ANDROID"
)