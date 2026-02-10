package com.umc.mobile.my4cut.data.invitation.model

data class WorkspaceInviteRequestDto(
    val workspaceId: Long,
    val userIds: List<Long>
)