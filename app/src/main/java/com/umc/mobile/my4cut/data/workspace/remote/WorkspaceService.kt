package com.umc.mobile.my4cut.data.workspace.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceCreateRequestDto
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceInfoResponseDto
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceUpdateRequest
import retrofit2.http.*

interface WorkspaceService {

    /** 워크스페이스 생성 */
    @POST("workspaces")
    suspend fun createWorkspace(
        @Body request: WorkspaceCreateRequestDto
    ): BaseResponse<WorkspaceInfoResponseDto>

    /** 내 워크스페이스 목록 조회 */
    @GET("workspaces/me")
    suspend fun getMyWorkspaces(): BaseResponse<List<WorkspaceInfoResponseDto>>

    /** 워크스페이스 상세 조회 */
    @GET("workspaces/{workspaceId}")
    suspend fun getWorkspaceDetail(
        @Path("workspaceId") workspaceId: Long
    ): BaseResponse<WorkspaceInfoResponseDto>

    /** 워크스페이스 수정 */
    @PATCH("workspaces/{workspaceId}")
    suspend fun updateWorkspace(
        @Path("workspaceId") workspaceId: Long,
        @Body request: WorkspaceUpdateRequest
    ): BaseResponse<WorkspaceInfoResponseDto>

    /** 워크스페이스 삭제 */
    @DELETE("workspaces/{workspaceId}")
    suspend fun deleteWorkspace(
        @Path("workspaceId") workspaceId: Long
    ): BaseResponse<Unit>
}