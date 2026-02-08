package com.umc.mobile.my4cut.ui.photo

data class PhotoData(
    val photoId: Long,      //  서버에서 내려준 사진 id
    val userImageRes: Int,  // 프로필 이미지 리소스 ID
    val userName: String,   // 이름
    val dateTime: String,   // 날짜
    val commentCount: Int,  // 댓글 수
    val photoImageRes: Int  // 메인 사진 리소스 ID
)
