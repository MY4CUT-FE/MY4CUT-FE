package com.umc.mobile.my4cut.data.workspace.model

data class WorkspaceDto(
    val id: Long,
    val name: String,
    val ownerId: Long,
    val createdAt: String,
    val expiresAt: String,
    val memberCount: Int,
    val memberProfiles: List<String>
)