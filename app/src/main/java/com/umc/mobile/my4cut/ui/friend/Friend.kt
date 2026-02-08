package com.umc.mobile.my4cut.ui.friend

data class Friend(
    val friendId: Long,
    val userId: Long,
    val nickname: String,
    var isFavorite: Boolean = false,
    var isSelected: Boolean = false
)