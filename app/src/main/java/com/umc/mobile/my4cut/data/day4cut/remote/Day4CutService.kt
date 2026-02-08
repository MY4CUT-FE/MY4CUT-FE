package com.umc.mobile.my4cut.data.day4cut.remote

import com.google.gson.annotations.SerializedName
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.ui.home.Day4CutResponse
import com.umc.mobile.my4cut.ui.home.Day4CutCalendarResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Day4CutService {

    @GET("day4cut")
    fun getDay4Cut(
        @Query("date") date: String
    ): Call<BaseResponse<Day4CutResponse>>

    @GET("day4cut/calendar")
    fun getCalendar(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Call<BaseResponse<Day4CutCalendarResponse>>

    /**
     * 하루네컷 생성
     * POST /day4cut
     */
    @POST("day4cut")
    fun createDay4Cut(
        @Body request: CreateDay4CutRequest
    ): Call<BaseResponse<String>>
}

// 하루네컷 생성 요청
data class CreateDay4CutRequest(
    val date: String, // "2026-02-07"
    val content: String?,
    val emojiType: String?, // "HAPPY" 등
    val images: List<Day4CutImage>
)

data class Day4CutImage(
    @SerializedName("mediaFileId") val mediaFileId: Int, // ✅ 변경
    @SerializedName("isThumbnail") val isThumbnail: Boolean
)