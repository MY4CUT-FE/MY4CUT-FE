package com.umc.mobile.my4cut.data.invitation.model

data class WorkspaceInvitationDto(
    val invitationId: Long,
    val workspaceId: Long,
    val workspaceName: String,
    val inviterNickname: String,
    val createdAt: String
)