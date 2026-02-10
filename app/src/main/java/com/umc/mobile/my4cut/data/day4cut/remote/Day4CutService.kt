package com.umc.mobile.my4cut.data.day4cut.remote

import com.google.gson.annotations.SerializedName
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.day4cut.model.CalendarStatusResponse
import com.umc.mobile.my4cut.data.day4cut.model.Day4CutDetailResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface Day4CutService {

    /**
     * 하루네컷 생성
     * POST /day4cut
     */
    @POST("day4cut")
    suspend fun createDay4Cut(
        @Body request: CreateDay4CutRequest
    ): BaseResponse<CreateDay4CutResponse>

    /**
     * 월별 기록 날짜 + 대표 이미지 조회
     * GET /day4cut/calendar
     */
    @GET("day4cut/calendar")
    suspend fun getCalendarStatus(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): BaseResponse<CalendarStatusResponse>

    /**
     * 날짜별 하루네컷 상세 조회
     * GET /day4cut?date=yyyy-MM-dd
     */
    @GET("day4cut")
    suspend fun getDay4CutDetail(
        @Query("date") date: String
    ): BaseResponse<Day4CutDetailResponse>

    /**
     * 하루네컷 수정
     * PATCH /day4cut
     */
    @PATCH("day4cut")
    suspend fun updateDay4Cut(
        @Body request: UpdateDay4CutRequest
    ): BaseResponse<UpdateDay4CutResponse>

    /**
     * 하루네컷 삭제
     * DELETE /day4cut?date=yyyy-MM-dd
     */
    @DELETE("day4cut")
    suspend fun deleteDay4Cut(
        @Query("date") date: String
    ): BaseResponse<DeleteDay4CutResponse>
}

/* ===== Request DTO ===== */

// 하루네컷 생성 요청
data class CreateDay4CutRequest(
    val date: String,          // "2026-02-07"
    val content: String?,
    val emojiType: String?,    // "HAPPY" 등
    val images: List<Day4CutImage>
)

// 하루네컷 수정 요청
data class UpdateDay4CutRequest(
    val date: String,          // "2026-02-07"
    val content: String?,
    val emojiType: String?,    // "HAPPY" 등
    val images: List<Day4CutImage>
)

data class Day4CutImage(
    @SerializedName("mediaFileId")
    val mediaFileId: Int,
    @SerializedName("isThumbnail")
    val isThumbnail: Boolean
)

/* ===== Response DTO ===== */

// 하루네컷 생성 응답
data class CreateDay4CutResponse(
    val success: Boolean
)

// 하루네컷 수정 응답
data class UpdateDay4CutResponse(
    val success: Boolean
)

// 하루네컷 삭제 응답
data class DeleteDay4CutResponse(
    val success: Boolean
)