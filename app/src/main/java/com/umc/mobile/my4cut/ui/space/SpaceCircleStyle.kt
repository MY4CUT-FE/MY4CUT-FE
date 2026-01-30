package com.umc.mobile.my4cut.ui.space

enum class SpaceCircleSize {
    LARGE, MEDIUM, SMALL
}

data class SpaceCircleStyle(
    val circleSizeDp: Int,
    val titleTextSp: Float,
    val memberTextSp: Float,
    val expireTextSp: Float
)

fun getStyle(size: SpaceCircleSize): SpaceCircleStyle {
    return when (size) {
        SpaceCircleSize.LARGE -> SpaceCircleStyle(
            circleSizeDp = 160,
            titleTextSp = 14f,
            memberTextSp = 12f,
            expireTextSp = 10f
        )
        SpaceCircleSize.MEDIUM -> SpaceCircleStyle(
            circleSizeDp = 120,
            titleTextSp = 12f,
            memberTextSp = 10f,
            expireTextSp = 9f
        )
        SpaceCircleSize.SMALL -> SpaceCircleStyle(
            circleSizeDp = 90,
            titleTextSp = 10f,
            memberTextSp = 9f,
            expireTextSp = 8f
        )
    }
}