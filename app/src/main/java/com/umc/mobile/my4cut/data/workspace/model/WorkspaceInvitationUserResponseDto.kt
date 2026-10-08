package com.umc.mobile.my4cut.data.workspace.model

data class WorkspaceInvitationUserResponseDto(
    val invitationId: Long,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val inviterId: Long,
    val inviteeId: Long,
    val status: String,
    val isFriend: Boolean,
    val isInviter: Boolean
)