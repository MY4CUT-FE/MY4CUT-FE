package com.umc.mobile.my4cut.ui.myalbum

import java.time.LocalDate
import java.io.Serializable

data class CalendarData(
    val date: LocalDate,        // 기준 날짜 (ID 역할)
    val imageUris: List<String>, // 사진 경로 (나중에 진짜 url은 Uri 객체)
    val memo: String = ""       // 메모
) : Serializable