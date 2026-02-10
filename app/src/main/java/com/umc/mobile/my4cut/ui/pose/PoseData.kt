package com.umc.mobile.my4cut.ui.pose

import com.google.gson.annotations.SerializedName

data class PoseData(
    @SerializedName("poseId") val poseId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("viewUrl") val imageUrl: String,
    @SerializedName("peopleCount") val peopleCount: Int,
    @SerializedName("isFavorite") var isFavorite: Boolean = false  // ✅ 서버에서 받아오기
)