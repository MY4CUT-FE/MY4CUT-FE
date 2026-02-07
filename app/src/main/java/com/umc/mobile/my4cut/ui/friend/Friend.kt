package com.umc.mobile.my4cut.ui.friend

data class Friend(
    val id: Long,
    val nickname: String,
    var isFavorite: Boolean = false,
    var isSelected: Boolean = false
)