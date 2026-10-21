package com.umc.mobile.my4cut.data.tutorial.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.tutorial.model.TutorialStatusResponseDto
import com.umc.mobile.my4cut.data.tutorial.model.TutorialType
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface TutorialService {

    /** 튜토리얼 상태 조회 */
    @GET("tutorials")
    suspend fun getTutorialStatus(): BaseResponse<TutorialStatusResponseDto>

    /** 튜토리얼 완료 처리 */
    @PATCH("tutorials/{tutorialType}/complete")
    suspend fun completeTutorial(
        @Path("tutorialType") tutorialType: TutorialType
    ): BaseResponse<TutorialStatusResponseDto>
}