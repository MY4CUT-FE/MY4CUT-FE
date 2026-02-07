package com.umc.mobile.my4cut.data.workspace.model

data class WorkspaceDto(
    val id: Long,
    val name: String,
    val currentMember: Int,
    val maxMember: Int,
    val createdAt: Long,
    val expiredAt: Long
)