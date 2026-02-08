package com.umc.mobile.my4cut.data.media.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MediaService {

    /**
     * 미디어 파일 업로드
     * POST /media/upload
     */
    @Multipart
    @POST("media/upload")
    fun uploadMedia(
        @Part file: MultipartBody.Part
    ): Call<BaseResponse<MediaUploadResponse>>
}

// 업로드 응답
data class MediaUploadResponse(
    val fileId: Int,
    val fileUrl: String
)