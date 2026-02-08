package com.umc.mobile.my4cut.ui.pose

import com.google.gson.annotations.SerializedName

data class PoseData(
    @SerializedName("poseId") val poseId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("peopleCount") val peopleCount: Int,
    var isFavorite: Boolean = false // 로컬에서 관리 (북마크 여부)
)