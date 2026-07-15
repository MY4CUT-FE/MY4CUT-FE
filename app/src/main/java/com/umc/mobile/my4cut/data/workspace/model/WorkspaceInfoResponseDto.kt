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
    val recentActivityType: String?,
    val recentActivityUserNickname: String?,
    val recentActivityAt: String?
)