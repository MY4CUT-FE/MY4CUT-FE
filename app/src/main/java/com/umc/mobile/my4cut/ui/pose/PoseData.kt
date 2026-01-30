package com.umc.mobile.my4cut.ui.pose

data class PoseData(
    val id: Int,
    val title: String,
    val peopleCount: Int, // 1: 1인, 2: 2인, 3: 3인, 4: 4인
    var isFavorite: Boolean = false // 즐겨찾기 여부
)