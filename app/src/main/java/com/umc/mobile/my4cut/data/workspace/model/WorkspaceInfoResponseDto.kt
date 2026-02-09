package com.umc.mobile.my4cut.data.workspace.model

data class WorkspaceInfoResponseDto(
    val id: Long,
    val name: String,
    val ownerId: Long,
    val expiresAt: String,
    val createdAt: String,
    val memberCount: Int?,
    val memberProfiles: List<String>?
)