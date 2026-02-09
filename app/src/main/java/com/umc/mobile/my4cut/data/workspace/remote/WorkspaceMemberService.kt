package com.umc.mobile.my4cut.data.workspace.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import retrofit2.http.DELETE
import retrofit2.http.Path

interface WorkspaceMemberService {

    /** 워크스페이스 나가기 */
    @DELETE("workspaces/{workspaceId}/members/me")
    suspend fun leaveWorkspace(
        @Path("workspaceId") workspaceId: Long
    ): BaseResponse<Unit>
}