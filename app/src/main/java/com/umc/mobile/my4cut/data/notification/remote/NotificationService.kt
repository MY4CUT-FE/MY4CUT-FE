package com.umc.mobile.my4cut.data.notification.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.notification.model.NotificationDto
import com.umc.mobile.my4cut.data.notification.model.NotificationReadResponseDto
import com.umc.mobile.my4cut.data.notification.model.RegisterTokenRequestDto
import com.umc.mobile.my4cut.data.notification.model.RegisterTokenResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationService {

    /** 알림 목록 조회 */
    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0
    ): BaseResponse<List<NotificationDto>>

    /** 읽음 처리 */
    @PATCH("notifications/{id}/read")
    suspend fun readNotification(
        @Path("id") id: Long
    ): BaseResponse<NotificationReadResponseDto>

    @POST("/notifications/token")
    suspend fun registerToken(
        @Body request: RegisterTokenRequestDto
    ): BaseResponse<RegisterTokenResponseDto>
}