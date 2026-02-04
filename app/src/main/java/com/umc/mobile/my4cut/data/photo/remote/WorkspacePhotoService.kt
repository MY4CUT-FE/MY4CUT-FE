package com.umc.mobile.my4cut.data.photo.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.photo.model.CommentCreateRequest
import com.umc.mobile.my4cut.data.photo.model.CommentDto
import com.umc.mobile.my4cut.data.photo.model.PhotoDto
import com.umc.mobile.my4cut.data.photo.model.PhotoUploadRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WorkspacePhotoService {

    @GET("workspaces/{workspaceId}/photos")
    suspend fun getPhotos(
        @Path("workspaceId") workspaceId: Long
    ): BaseResponse<List<PhotoDto>>

    @POST("workspaces/{workspaceId}/photos")
    suspend fun upload(
        @Path("workspaceId") workspaceId: Long,
        @Body request: PhotoUploadRequest
    ): BaseResponse<PhotoDto>

    @GET("workspaces/{workspaceId}/photos/{photoId}/comments")
    suspend fun getComments(
        @Path("workspaceId") workspaceId: Long,
        @Path("photoId") photoId: Long
    ): BaseResponse<List<CommentDto>>

    @POST("workspaces/{workspaceId}/photos/{photoId}/comments")
    suspend fun addComment(
        @Path("workspaceId") workspaceId: Long,
        @Path("photoId") photoId: Long,
        @Body request: CommentCreateRequest
    ): BaseResponse<CommentDto>

    @DELETE("workspaces/{workspaceId}/photos/{photoId}")
    suspend fun deletePhoto(
        @Path("workspaceId") workspaceId: Long,
        @Path("photoId") photoId: Long
    ): BaseResponse<Unit>
}