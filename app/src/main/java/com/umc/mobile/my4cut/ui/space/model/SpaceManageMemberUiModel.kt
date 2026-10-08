package com.umc.mobile.my4cut.ui.space.model

enum class SpaceManageMemberStatus {
    MEMBER,      // 스페이스 참여 중
    PENDING,     // 초대 수락 대기
    REJECTED     // 초대 거절
}

data class SpaceManageMemberUiModel(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
    val status: SpaceManageMemberStatus,

    // 현재 나와 친구인지
    val isFriend: Boolean,

    // 이 사람을 내가 초대했는지
    val invitedByMe: Boolean,

    // 초대 취소 API에서 사용할 ID
    val invitationId: Long? = null
)