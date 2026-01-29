package com.example.my4cut

data class PhotoData(
    val userImageRes: Int,  // 프로필 이미지 리소스 ID
    val userName: String,   // 이름
    val dateTime: String,   // 날짜
    val commentCount: Int,  // 댓글 수
    val photoImageRes: Int  // 메인 사진 리소스 ID
)
