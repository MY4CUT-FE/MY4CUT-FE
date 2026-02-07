package com.umc.mobile.my4cut.data.photo.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.photo.model.CommentCreateRequest
import com.umc.mobile.my4cut.data.photo.model.CommentDto
import com.umc.mobile.my4cut.data.photo.model.PhotoDto
import okhttp3.MultipartBody
import retrofit2.http.*

interface WorkspacePhotoService {

    /** 사진 목록 조회 */
    @GET("workspaces/{workspaceId}/photos")
    suspend fun getPhotos(
        @Path("workspaceId") workspaceId: Long
    ): BaseResponse<List<PhotoDto>>

    /** 사진 업로드 */
    @Multipart
    @POST("workspaces/{workspaceId}/photos")
    suspend fun uploadPhoto(
        @Path("workspaceId") workspaceId: Long,
        @Part image: MultipartBody.Part
    ): BaseResponse<PhotoDto>

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