package com.umc.mobile.my4cut.data.workspace.model

data class WorkspaceCreateRequest(
    val name: String,
    val memberIds: List<Long>
)