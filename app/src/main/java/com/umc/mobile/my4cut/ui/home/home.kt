package com.umc.mobile.my4cut.ui.home

import com.google.gson.annotations.SerializedName

// 하루네컷 상세 조회 응답
data class Day4CutResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("fileUrl") val fileUrl: List<String>,
    @SerializedName("content") val content: String?,
    @SerializedName("emojiType") val emojiType: String? // "HAPPY" 등
)

// 캘린더 응답
data class Day4CutCalendarResponse(
    @SerializedName("dates") val dates: List<CalendarDate>
)

data class CalendarDate(
    @SerializedName("day") val day: Int,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String?
)