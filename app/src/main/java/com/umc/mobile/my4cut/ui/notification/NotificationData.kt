package com.umc.mobile.my4cut.ui.notification

data class NotificationData(
    val id: Long,             // notificationId: 알림 자체의 ID

    val referenceId: Long,    // 알림 대상 ID
    // FRIEND_REQUEST -> friendRequestId
    // WORKSPACE_INVITE -> invitationId

    val mediaId: Long?,       // 댓글이 달린 사진 또는 업로드된 사진의 mediaId

    val workspaceId: Long?,   // 워크스페이스 관련 알림의 workspaceId
    // MEDIA_COMMENT, MEDIA_UPLOADED,
    // WORKSPACE_INVITE, WORKSPACE_ACCEPTED에서 사용

    val senderId: Long?,      // 알림을 발생시킨 사용자 ID
    // FRIEND_ACCEPTED 등 친구 관련 화면 이동 시 사용

    val type: String,         // 알림 타입
    // FRIEND_REQUEST
    // FRIEND_ACCEPTED
    // WORKSPACE_INVITE
    // WORKSPACE_ACCEPTED
    // MEDIA_COMMENT
    // MEDIA_UPLOADED

    val iconResId: Int,       // 알림 아이콘 리소스 ID

    val category: String,     // 화면에 표시할 카테고리
    // 친구, 초대, 댓글, 사진

    val content: String,      // 알림 내용

    val time: String,         // "13분 전", "2일 전" 등의 표시 시간

    val hasButtons: Boolean   // 수락/거절 버튼 표시 여부
    // FRIEND_REQUEST, WORKSPACE_INVITE만 true
)