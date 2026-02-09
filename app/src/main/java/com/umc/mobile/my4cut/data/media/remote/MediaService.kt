package com.umc.mobile.my4cut.data.media.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MediaService {

    /**
     * 미디어 파일 bulk 업로드
     * POST /media/upload/bulk
     */
    @Multipart
    @POST("media/upload/bulk")
    suspend fun uploadMediaBulk(
        @Part files: List<MultipartBody.Part>
    ): BaseResponse<List<MediaBulkUploadItem>>
}

// Bulk 업로드 응답 아이템
data class MediaBulkUploadItem(
    val fileId: Int,
    val fileKey: String,
    val viewUrl: String
)