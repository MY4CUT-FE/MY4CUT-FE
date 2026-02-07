package com.umc.mobile.my4cut.ui.space

data class Space(
    val id: Long,
    val name: String,
    val currentMember: Int,
    val maxMember: Int = 10,
    val createdAt: Long,
    val expiredAt: Long
)