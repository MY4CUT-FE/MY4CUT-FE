package com.umc.mobile.my4cut.data.auth.remote

import com.umc.mobile.my4cut.data.auth.model.EmailCheckResponse
import com.umc.mobile.my4cut.data.auth.model.KakaoLoginRequest
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.auth.model.LoginRequest
import com.umc.mobile.my4cut.data.auth.model.SignUpRequest
import com.umc.mobile.my4cut.data.auth.model.TokenResult
import retrofit2.Call
import retrofit2.http.*

interface AuthService {

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("auth/signup")
    fun signUp(
        @Body request: SignUpRequest
    ): Call<BaseResponse<Any>>

    /**
     * ✅ 이메일 중복 체크 (POST 방식)
     * POST /auth/check-email
     */
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("auth/check-email")
    fun checkEmailDuplicatePost(
        @Body request: Map<String, String>
    ): Call<BaseResponse<EmailCheckResponse>>

    /**
     * ✅ 이메일 중복 체크 (GET 방식)
     * GET /auth/check-email?email={email}
     */
    @GET("auth/check-email")
    fun checkEmailDuplicateGet(
        @Query("email") email: String
    ): Call<BaseResponse<EmailCheckResponse>>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<BaseResponse<TokenResult>>

    @POST("auth/refresh")
    fun refresh(
        @Header("Authorization") authorization: String  // ✅ Authorization 헤더 사용
    ): Call<BaseResponse<TokenResult>>

    @POST("auth/kakao")
    fun loginKakao(
        @Body request: KakaoLoginRequest
    ): Call<BaseResponse<TokenResult>>

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @DELETE("auth/withdraw")
    fun withdraw(): Call<BaseResponse<String>>
}