package com.umc.mobile.my4cut.data.album.remote

import com.umc.mobile.my4cut.data.album.model.AlbumDetailResponse
import com.umc.mobile.my4cut.data.album.model.AlbumNameRequest
import com.umc.mobile.my4cut.data.album.model.AlbumRequest
import com.umc.mobile.my4cut.data.album.model.AlbumResponse
import com.umc.mobile.my4cut.data.base.BaseResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AlbumService {
    @GET("/albums")
    suspend fun getAlbums(): BaseResponse<List<AlbumResponse>>

    @GET("/albums/{albumId}")
    suspend fun getAlbumDetail(
        @Path("albumId") albumId: Int
    ): BaseResponse<AlbumDetailResponse>
    @POST("/albums")
    suspend fun createAlbum(
        @Body nameRequest: AlbumNameRequest
    ): BaseResponse<AlbumResponse>

    @POST("/albums/{albumId}/photos")
    suspend fun addPhotosToAlbum(
        @Path("albumId") albumId: Int,
        @Body request: AlbumRequest
    ): BaseResponse<AlbumDetailResponse>
}