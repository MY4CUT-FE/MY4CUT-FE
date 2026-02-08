package com.umc.mobile.my4cut.data.day4cut.model

data class Day4CutDetailResponse(
    val id: Int,
    val fileUrl: List<String>,
    val content: String,
    val emojiType: String
)