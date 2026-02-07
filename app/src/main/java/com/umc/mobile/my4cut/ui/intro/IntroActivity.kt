package com.umc.mobile.my4cut.ui.intro

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.auth.model.KakaoLoginRequest
import com.umc.mobile.my4cut.data.auth.model.TokenResult
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.ActivityIntroBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.login.LoginActivity
import com.umc.mobile.my4cut.ui.signup.SignUpActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 회원가입 버튼 밑줄
        binding.tvSignup.paintFlags =
            binding.tvSignup.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        initClickListener()

        // 앱 실행 시 자동 로그인 시도
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        val savedRefreshToken = TokenManager.getRefreshToken(this)
        if (savedRefreshToken.isNullOrEmpty()) return

        val headerToken = "Bearer $savedRefreshToken"

        // 인터셉터가 없는 NoAuth 서비스를 사용
        RetrofitClient.authServiceNoAuth.refresh(headerToken)
            .enqueue(object : Callback<BaseResponse<TokenResult>> {
                override fun onResponse(
                    call: Call<BaseResponse<TokenResult>>,
                    response: Response<BaseResponse<TokenResult>>
                ) {
                    if (response.isSuccessful) {
                        val resp = response.body()
                        if (resp != null && (resp.code == "SUCCESS" || resp.code == "COMMON200")) {
                            val newTokens = resp.data
                            if (newTokens != null) {
                                // 새로운 토큰 저장
                                TokenManager.saveTokens(
                                    this@IntroActivity,
                                    newTokens.accessToken,
                                    newTokens.refreshToken
                                )

                                Toast.makeText(this@IntroActivity, "자동 로그인 되었습니다.", Toast.LENGTH_SHORT).show()

                                val intent = Intent(this@IntroActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            }
                        } else {
                            Log.d("AutoLogin", "Refresh Failed: ${resp?.message}")
                            TokenManager.clear(this@IntroActivity)
                        }
                    }
                }

                override fun onFailure(call: Call<BaseResponse<TokenResult>>, t: Throwable) {
                    Log.e("AutoLogin", "Network Error", t)
                }
            })
    }

    private fun initClickListener() {
        // 회원가입 버튼 클릭 -> SignUpActivity로 이동
        binding.btnLoginMy4cut.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 카카오 로그인 버튼
        binding.btnLoginKakao.setOnClickListener {
            startKakaoLogin()
        }

        // 마이포컷 로그인 버튼 클릭 -> LoginActivity 이동
        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    // 카카오 로그인 시작
    private fun startKakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("KakaoLogin", "카카오 로그인 실패", error)
            } else if (token != null) {
                Log.d("KakaoLogin", "accessToken = ${token.accessToken}")
                sendKakaoTokenToServer(token.accessToken)
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    if (error is ClientError &&
                        error.reason == ClientErrorCause.Cancelled
                    ) return@loginWithKakaoTalk

                    UserApiClient.instance.loginWithKakaoAccount(
                        this,
                        callback = callback
                    )
                } else if (token != null) {
                    sendKakaoTokenToServer(token.accessToken)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(
                this,
                callback = callback
            )
        }
    }

    // 서버로 카카오 accessToken 전송
    private fun sendKakaoTokenToServer(kakaoAccessToken: String) {
        val request = KakaoLoginRequest(accessToken = kakaoAccessToken)

        RetrofitClient.authService
            .loginKakao(request)
            .enqueue(object : Callback<BaseResponse<TokenResult>> {

                override fun onResponse(
                    call: Call<BaseResponse<TokenResult>>,
                    response: Response<BaseResponse<TokenResult>>
                ) {
                    Log.d("KakaoServer", "code=${response.code()}")
                    Log.d("KakaoServer", "body=${response.body()}")
                    Log.d("KakaoServer", "error=${response.errorBody()?.string()}")

                    if (response.isSuccessful) {
                        val tokenResult = response.body()?.data

                        if (tokenResult != null) {

                            Log.d("AUTH", "서버 accessToken = ${tokenResult.accessToken}")
                            Log.d("AUTH", "서버 refreshToken = ${tokenResult.refreshToken}")

                            // 토큰은 TokenManager로 저장
                            TokenManager.saveTokens(
                                this@IntroActivity,
                                tokenResult.accessToken,
                                tokenResult.refreshToken
                            )

                            // 사용자 정보만 UserPrefs
                            saveUserInfo(
                                tokenResult.userId,
                                "KAKAO"
                            )

                            val intent = Intent(this@IntroActivity, MainActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        else {
                            // 신규 회원 → 회원가입
                            val intent =
                                Intent(this@IntroActivity, SignUpActivity::class.java)
                            intent.putExtra("loginType", "KAKAO")
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(
                            this@IntroActivity,
                            "카카오 로그인 실패 (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<TokenResult>>, t: Throwable) {
                    Log.e("KakaoServer", "통신 실패", t)
                    Toast.makeText(
                        this@IntroActivity,
                        "네트워크 오류",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun saveUserInfo(userId: Int, loginType: String) {
        val spf = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        spf.edit()
            .putInt("userId", userId)
            .putString("loginType", loginType)
            .apply()
    }

}
