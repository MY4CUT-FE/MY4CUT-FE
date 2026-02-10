package com.umc.mobile.my4cut.data.invitation.model

data class WorkspaceInvitationDto(
    val invitationId: Long,
    val workspaceName: String?,
    val inviterNickname: String?,
    val status: String?,
    val createdAt: String?
)