package com.umc.mobile.my4cut.ui.space.model

data class CreateSpaceResult(
    val spaceName: String,
    val currentMember: Int,
    val maxMember: Int
)