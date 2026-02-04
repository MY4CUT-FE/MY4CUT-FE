package com.umc.mobile.my4cut.data.invitation.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.invitation.model.WorkspaceInvitationDto
import com.umc.mobile.my4cut.data.invitation.model.WorkspaceInviteRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WorkspaceInvitationService {

    @POST("workspaces/invitations")
    suspend fun invite(
        @Body request: WorkspaceInviteRequest
    ): BaseResponse<Unit>

    @POST("workspaces/invitations/{invitationId}/accept")
    suspend fun accept(
        @Path("invitationId") invitationId: Long
    ): BaseResponse<Unit>

    @POST("workspaces/invitations/{invitationId}/reject")
    suspend fun reject(
        @Path("invitationId") invitationId: Long
    ): BaseResponse<Unit>

    @GET("workspaces/invitations/me")
    suspend fun getMyInvitations():
            BaseResponse<List<WorkspaceInvitationDto>>
}