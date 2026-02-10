package com.umc.mobile.my4cut.data.invitation.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.invitation.model.WorkspaceInvitationDto
import com.umc.mobile.my4cut.data.invitation.model.WorkspaceInviteRequestDto
import retrofit2.http.*

interface WorkspaceInvitationService {

    /** 멤버 초대 */
    @POST("workspaces/invitations")
    suspend fun inviteMember(
        @Body request: WorkspaceInviteRequestDto
    ): BaseResponse<Unit>

    /** 초대 수락 */
    @POST("workspaces/invitations/{invitationId}/accept")
    suspend fun acceptInvitation(
        @Path("invitationId") invitationId: Long
    ): BaseResponse<Unit>

    /** 초대 거절 */
    @POST("workspaces/invitations/{invitationId}/reject")
    suspend fun rejectInvitation(
        @Path("invitationId") invitationId: Long
    ): BaseResponse<Unit>

    /** 내가 받은 초대 목록 조회 */
    @GET("workspaces/invitations/me")
    suspend fun getMyInvitations(): BaseResponse<List<WorkspaceInvitationDto>>
}