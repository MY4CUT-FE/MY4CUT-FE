package com.umc.mobile.my4cut.data.image.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Url

interface ImageService {

    /**
     * Presigned URL 발급
     * POST /images/presigned-url
     */
    @POST("images/presigned-url")
    fun getPresignedUrl(
        @Body request: PresignedUrlRequest
    ): Call<BaseResponse<PresignedUrlResponse>>

    /**
     * S3에 직접 업로드 (Presigned URL 사용)
     * 주의: 이 요청은 인증 헤더가 필요 없음
     */
    @PUT
    fun uploadToS3(
        @Url uploadUrl: String,
        @Body imageBody: RequestBody
    ): Call<Void>
}

// Request 모델
data class PresignedUrlRequest(
    val type: String, // "CALENDAR"
    val fileName: String,
    val contentType: String // "image/jpeg", "image/png" 등
)

// Response 모델
data class PresignedUrlResponse(
    val mediaId: Int, // ✅ 이게 곧 mediaFileId
    val uploadUrl: String, // S3 업로드 URL (5분 유효)
    val fileKey: String
)