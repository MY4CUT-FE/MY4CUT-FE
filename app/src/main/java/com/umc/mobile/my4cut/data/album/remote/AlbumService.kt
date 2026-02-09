package com.umc.mobile.my4cut.data.album.remote

import com.umc.mobile.my4cut.data.album.model.AlbumDetailResponse
import com.umc.mobile.my4cut.data.album.model.AlbumNameRequest
import com.umc.mobile.my4cut.data.album.model.AlbumRequest
import com.umc.mobile.my4cut.data.album.model.AlbumResponse
import com.umc.mobile.my4cut.data.base.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AlbumService {
    // 앨범 목록 조회
    @GET("/albums")
    suspend fun getAlbums(): BaseResponse<List<AlbumResponse>>

    // 앨범 상세 정보 조회
    @GET("/albums/{albumId}")
    suspend fun getAlbumDetail(
        @Path("albumId") albumId: Int
    ): BaseResponse<AlbumDetailResponse>

    // 앨범 생성
    @POST("/albums")
    suspend fun createAlbum(
        @Body nameRequest: AlbumNameRequest
    ): BaseResponse<AlbumResponse>

    // 앨범에 사진 추가
    @POST("/albums/{albumId}/photos")
    suspend fun addPhotosToAlbum(
        @Path("albumId") albumId: Int,
        @Body request: AlbumRequest
    ): BaseResponse<AlbumDetailResponse>

    // 앨범 이름 수정
    @PATCH("/albums/{albumId}")
    suspend fun updateAlbumName(
        @Path("albumId") albumId: Int,
        @Body nameRequest: AlbumNameRequest
    ): Response<BaseResponse<AlbumResponse>>

    // 앨범 삭제
    @DELETE("/albums/{albumId}")
    suspend fun deleteAlbum(
        @Path("albumId") albumId: Int
    ): Response<BaseResponse<String>>
}