package com.example.my4cut

data class Friend(
    val id: Int,
    val nickname: String,
    var isFavorite: Boolean = false,
    var isSelected: Boolean = false
)