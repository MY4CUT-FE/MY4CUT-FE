package com.umc.mobile.my4cut.ui.photo

data class PhotoData(
    val photoId: Long,      //  서버에서 내려준 사진 id
    val userProfileUrl: String?,  // 프로필 이미지 URL (API)
    val userName: String,   // 이름
    val dateTime: String,   // 날짜
    val commentCount: Int,  // 댓글 수
    // 기존 로컬 이미지 (미리보기용)
    val photoImageRes: Int? = null,

    // 서버 이미지 URL (nullable)
    val photoUrl: String?,
    val uploaderId: Long?
)
