package com.umc.mobile.my4cut.data.day4cut.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.day4cut.model.CalendarStatusResponse
import com.umc.mobile.my4cut.data.day4cut.model.Day4CutDetailResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface Day4CutService {
    // 월별 기록 날짜와 대표 이미지 URL 리스트 조회
    @GET("/day4cut/calendar")
    suspend fun getCalendarStatus(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): BaseResponse<CalendarStatusResponse> // 날짜와 이미지URL을 포함한 객체 리스트

    // 날짜로 상세 조회
    @GET("/day4cut")
    suspend fun getDay4CutDetail(
        @Query("date") date: String // "2026-02-07" 형식
    ): BaseResponse<Day4CutDetailResponse>
}