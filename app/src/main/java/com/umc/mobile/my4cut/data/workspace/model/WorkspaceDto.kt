package com.umc.mobile.my4cut.data.workspace.model

data class WorkspaceDto(
    val id: Long,
    val name: String,
    val coverImageUrl: String?,
    val memberCount: Int,
    val isOwner: Boolean
)