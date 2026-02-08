package com.umc.mobile.my4cut.data.album.remote

import com.umc.mobile.my4cut.data.album.model.UploadMediaData
import com.umc.mobile.my4cut.data.base.BaseResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageService {
    @Multipart
    @POST("/media/upload/bulk")
    suspend fun uploadImagesMedia(
        @Part files: List<MultipartBody.Part>
    ): BaseResponse<List<UploadMediaData>>
}