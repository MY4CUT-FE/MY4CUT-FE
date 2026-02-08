package com.umc.mobile.my4cut.data.auth.remote

import com.umc.mobile.my4cut.data.auth.model.KakaoLoginRequest
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.auth.model.LoginRequest
import com.umc.mobile.my4cut.data.auth.model.SignUpRequest
import com.umc.mobile.my4cut.data.auth.model.TokenResult
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthService {

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("auth/signup")
    fun signUp(
        @Body request: SignUpRequest
    ): Call<BaseResponse<Any>>

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