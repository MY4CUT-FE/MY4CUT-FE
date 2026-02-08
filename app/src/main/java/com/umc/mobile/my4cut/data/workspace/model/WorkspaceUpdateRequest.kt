package com.umc.mobile.my4cut.data.workspace.model

data class WorkspaceUpdateRequest(
    val name: String,
    val memberIds: List<Long>
)