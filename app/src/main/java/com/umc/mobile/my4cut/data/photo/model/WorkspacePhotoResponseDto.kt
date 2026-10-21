package com.umc.mobile.my4cut.data.photo.model

data class WorkspacePhotoResponseDto(
    val mediaId: Long,
    val fileKey: String?,
    val viewUrl: String?,
    val mediaType: String?,
    val takenDate: String?,
    val isFinal: Boolean?,
    val createdAt: String?,
    val uploaderId: Long?,
    val uploaderNickname: String?,
    val uploaderProfileImageUrl: String?
)