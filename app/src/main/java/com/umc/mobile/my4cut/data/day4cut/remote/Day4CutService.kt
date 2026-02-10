package com.umc.mobile.my4cut.data.day4cut.remote

import com.google.gson.annotations.SerializedName
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.day4cut.model.CalendarStatusResponse
import com.umc.mobile.my4cut.data.day4cut.model.Day4CutDetailResponse
import retrofit2.http.Body
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
    ): BaseResponse<CalendarStatusResponse> // 날짜와 이미지URL을 포함한 객체 리스트

    /**
     * 날짜별 하루네컷 상세 조회
     * GET /day4cut?date=yyyy-MM-dd
     */
    @GET("day4cut")
    suspend fun getDay4CutDetail(
        @Query("date") date: String // "2026-02-07" 형식
    ): BaseResponse<Day4CutDetailResponse>

    /**
     * 하루네컷 수정 (PATCH)
     */
    @PATCH("day4cut")
    suspend fun updateDay4Cut(
        @Body request: CreateDay4CutRequest
    ): BaseResponse<CreateDay4CutResponse>
}

/* ===== Request DTO ===== */

// 하루네컷 생성 요청
data class CreateDay4CutRequest(
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