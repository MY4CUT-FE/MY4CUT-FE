package com.umc.mobile.my4cut.ui.photo

data class CommentData(
    val commentId: Long,
    val profileImgUrl: String? = null,
    val userName: String,
    val time: String,
    val content: String,
    val isMine: Boolean
)