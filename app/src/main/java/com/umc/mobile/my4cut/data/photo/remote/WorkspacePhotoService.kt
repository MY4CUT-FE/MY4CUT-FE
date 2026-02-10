package com.umc.mobile.my4cut.data.photo.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.photo.model.CommentCreateRequest
import com.umc.mobile.my4cut.data.photo.model.CommentDto
import com.umc.mobile.my4cut.data.photo.model.WorkspacePhotoResponseDto
import com.umc.mobile.my4cut.data.photo.model.WorkspacePhotoUploadRequestDto
import retrofit2.http.*

interface WorkspacePhotoService {

    /** 사진 목록 조회 */
    @GET("workspaces/{workspaceId}/photos")
    suspend fun getPhotos(
        @Path("workspaceId") workspaceId: Long
    ): BaseResponse<List<WorkspacePhotoResponseDto>>

    /** 사진 업로드 (mediaId 등록 방식) */
    @POST("workspaces/{workspaceId}/photos")
    suspend fun uploadPhotos(
        @Path("workspaceId") workspaceId: Long,
        @Body request: WorkspacePhotoUploadRequestDto
    ): BaseResponse<List<WorkspacePhotoResponseDto>>

    /** 댓글 목록 조회 */
    @GET("workspaces/{workspaceId}/photos/{photoId}/comments")
    suspend fun getComments(
        @Path("workspaceId") workspaceId: Long,
        @Path("photoId") photoId: Long
    ): BaseResponse<List<CommentDto>>

    /** 댓글 등록 */
    @POST("workspaces/{workspaceId}/photos/{photoId}/comments")
    suspend fun createComment(
        @Path("workspaceId") workspaceId: Long,
        @Path("photoId") photoId: Long,
        @Body request: CommentCreateRequest
    ): BaseResponse<CommentDto>

    /** 댓글 삭제 */
    @DELETE("workspaces/{workspaceId}/photos/{photoId}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("workspaceId") workspaceId: Long,
        @Path("photoId") photoId: Long,
        @Path("commentId") commentId: Long
    ): BaseResponse<Unit>

    /** 사진 삭제 */
    @DELETE("workspaces/{workspaceId}/photos/{id}")
    suspend fun deletePhoto(
        @Path("workspaceId") workspaceId: Long,
        @Path("id") photoId: Long
    ): BaseResponse<Unit>
}