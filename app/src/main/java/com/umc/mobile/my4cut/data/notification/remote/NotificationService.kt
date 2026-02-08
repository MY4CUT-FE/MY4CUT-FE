package com.umc.mobile.my4cut.data.notification.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.notification.model.NotificationDto
import com.umc.mobile.my4cut.data.notification.model.NotificationReadResponseDto
import retrofit2.http.GET
import retrofit2.http.PATCH
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
}