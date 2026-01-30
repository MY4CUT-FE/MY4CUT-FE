package com.umc.mobile.my4cut.ui.calendar

import java.time.LocalDate

data class CalendarData(
    val date: LocalDate,
    val imageUrl: Int?, // 이미지 리소스 ID (없으면 null or 0)
    val description: String? = null
)