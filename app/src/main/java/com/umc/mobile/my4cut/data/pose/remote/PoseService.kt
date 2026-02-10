package com.umc.mobile.my4cut.data.pose.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.ui.pose.PoseData
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PoseService {

    /**
     * 포즈 목록 조회
     * GET /poses
     * @param sort 정렬 기준 (옵션)
     * @param peopleCount 인원수 필터 (옵션: 1~4)
     */
    @GET("poses")
    fun getPoses(
        @Query("sort") sort: String? = null,
        @Query("peopleCount") peopleCount: Int? = null
    ): Call<BaseResponse<List<PoseData>>>

    /**
     * 포즈 상세 조회
     * GET /poses/{poseId}
     */
    @GET("poses/{poseId}")
    fun getPoseDetail(
        @Path("poseId") poseId: Int
    ): Call<BaseResponse<PoseData>>

    /**
     * 포즈 즐겨찾기 등록
     * POST /poses/{id}/bookmarks
     */
    @POST("poses/{id}/bookmarks")
    fun addBookmark(
        @Path("id") poseId: Int
    ): Call<BaseResponse<Any>>  // ✅ String → Any로 변경

    /**
     * 포즈 즐겨찾기 해제
     * DELETE /poses/{id}/bookmarks
     */
    @DELETE("poses/{id}/bookmarks")
    fun removeBookmark(
        @Path("id") poseId: Int
    ): Call<BaseResponse<Any>>  // ✅ String → Any로 변경
}