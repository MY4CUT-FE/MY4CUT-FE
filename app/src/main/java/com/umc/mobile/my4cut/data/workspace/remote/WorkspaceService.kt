package com.umc.mobile.my4cut.data.invitation.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceCreateRequest
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceDto
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceUpdateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface WorkspaceService {

    @POST("workspaces")
    suspend fun create(
        @Body request: WorkspaceCreateRequest
    ): BaseResponse<WorkspaceDto>

    @GET("workspaces/me")
    suspend fun getMyWorkspaces(): BaseResponse<List<WorkspaceDto>>

    @GET("workspaces/{workspaceId}")
    suspend fun getDetail(
        @Path("workspaceId") workspaceId: Long
    ): BaseResponse<WorkspaceDto>

    @PATCH("workspaces/{workspaceId}")
    suspend fun update(
        @Path("workspaceId") workspaceId: Long,
        @Body request: WorkspaceUpdateRequest
    ): BaseResponse<WorkspaceDto>

    @DELETE("workspaces/{workspaceId}")
    suspend fun delete(
        @Path("workspaceId") workspaceId: Long
    ): BaseResponse<Unit>
}