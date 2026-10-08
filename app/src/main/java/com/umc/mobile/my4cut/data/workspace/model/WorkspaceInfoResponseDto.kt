package com.umc.mobile.my4cut.data.workspace.model

data class WorkspaceInfoResponseDto(
    val id: Long,
    val name: String,
    val ownerId: Long,
    val expiresAt: String,
    val createdAt: String,
    val isFinal: Boolean?,
    val memberCount: Int?,
    val memberIds: List<Long>?,
    val memberProfiles: List<String>?,
    val pendingInvitationUserIds: List<Long>?,
    val alreadyInvitedFriendIds: List<Long>,

    // 스페이스 초대 사용자 정보
    val invitationUsers: List<WorkspaceInvitationUserResponseDto>? = null,

    val recentActivityType: String?,
    val recentActivityUserNickname: String?,
    val recentActivityAt: String?
)