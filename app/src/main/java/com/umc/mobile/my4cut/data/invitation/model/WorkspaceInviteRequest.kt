package com.umc.mobile.my4cut.data.invitation.model

data class WorkspaceInviteRequest(
    val workspaceId: Long,
    val targetUserId: Long
)