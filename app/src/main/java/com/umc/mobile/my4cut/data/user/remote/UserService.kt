package com.umc.mobile.my4cut.data.user.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part

interface UserService {

    /** 마이페이지 조회 */
    @GET("users/me")
    fun getMyPage(): Call<BaseResponse<UserMeResponse>>

    /** 닉네임 변경 */
    @PATCH("users/me/nickname")
    fun updateNickname(
        @Body request: NicknameRequest
    ): Call<BaseResponse<UserMeResponse>>

    /** 프로필 이미지 변경 */
    @PATCH("users/me/image")
    fun updateProfileImage(
        @Body request: ProfileImageRequest
    ): Call<BaseResponse<UserMeResponse>>

}