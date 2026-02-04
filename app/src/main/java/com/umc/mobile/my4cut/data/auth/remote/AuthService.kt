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

    // 1. 회원가입
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("auth/signup")
    fun signUp(
        @Body request: SignUpRequest
    ): Call<BaseResponse<Any>>

    // 2. 일반 로그인 (응답 data가 TokenResult 객체)
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<BaseResponse<TokenResult>>

    // 3. 토큰 재발급 (헤더에 Authorization 필요)
    @POST("auth/refresh")
    fun refresh(@Header("Authorization") token: String): Call<BaseResponse<TokenResult>>

    // 4. 카카오 로그인
    @POST("auth/kakao")
    fun loginKakao(
        @Body request: KakaoLoginRequest
    ): Call<BaseResponse<TokenResult>>

    // 5. 회원 탈퇴
    @DELETE("auth/withdraw")
    fun withdraw(
        @Header("Authorization") token: String
    ): Call<BaseResponse<String>>

}