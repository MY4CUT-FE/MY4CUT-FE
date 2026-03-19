package com.umc.mobile.my4cut.ui.intro

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
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

        binding.tvSignup.paintFlags = binding.tvSignup.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        initClickListener()
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        // 로그인 UI 숨기기 (자동 로그인 결과에 따라 다시 표시)
        setLoginUiVisibility(false)

        // access token이 유효하면 바로 이동 (네트워크 요청 불필요)
        if (TokenManager.isAccessTokenValid(this)) {
            Log.d("AutoLogin", "Access token valid, skipping login")
            navigateToMain()
            return
        }

        // refresh token으로 갱신 시도
        val savedRefreshToken = TokenManager.getRefreshToken(this)
        if (savedRefreshToken.isNullOrEmpty()) {
            setLoginUiVisibility(true)
            return
        }

        RetrofitClient.authServiceNoAuth.refresh("Bearer $savedRefreshToken")
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
                                TokenManager.saveTokens(
                                    this@IntroActivity,
                                    newTokens.accessToken,
                                    newTokens.refreshToken
                                )
                                Log.d("AutoLogin", "Token refreshed, navigating to main")
                                navigateToMain()
                                return
                            }
                        }
                    }
                    Log.d("AutoLogin", "Refresh failed: ${response.body()?.message}")
                    TokenManager.clear(this@IntroActivity)
                    setLoginUiVisibility(true)
                }

                override fun onFailure(call: Call<BaseResponse<TokenResult>>, t: Throwable) {
                    Log.e("AutoLogin", "Network error during refresh", t)
                    // 네트워크 오류 시 토큰은 유지하고 로그인 화면만 표시
                    setLoginUiVisibility(true)
                }
            })
    }

    private fun setLoginUiVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.INVISIBLE
        binding.btnLoginMy4cut.visibility = visibility
        binding.btnLoginKakao.visibility = visibility
        binding.tvSignup.visibility = visibility
    }

    private fun navigateToMain() {
        val intent = Intent(this@IntroActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

    }

    private fun initClickListener() {
        binding.btnLoginMy4cut.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnLoginKakao.setOnClickListener {
            startKakaoLogin()
        }

        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun startKakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("KakaoLogin", "카카오 로그인 실패", error)
                Toast.makeText(this, "로그인 실패: ${error.message}", Toast.LENGTH_LONG).show()
            } else if (token != null) {
                sendKakaoTokenToServer(token.accessToken)
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }

                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    sendKakaoTokenToServer(token.accessToken)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    private fun sendKakaoTokenToServer(kakaoAccessToken: String) {
        Log.d("KakaoLogin", "📤 Sending Kakao token to server: $kakaoAccessToken")

        val request = KakaoLoginRequest(accessToken = kakaoAccessToken)

        RetrofitClient.authServiceNoAuth.loginKakao(request)
            .enqueue(object : Callback<BaseResponse<TokenResult>> {

                override fun onResponse(
                    call: Call<BaseResponse<TokenResult>>,
                    response: Response<BaseResponse<TokenResult>>
                ) {
                    Log.d("KakaoServer", "📥 Response Code: ${response.code()}")
                    Log.d("KakaoServer", "📥 Is Successful: ${response.isSuccessful}")
                    Log.d("KakaoServer", "📥 Response Body: ${response.body()}")
                    Log.d("KakaoServer", "📥 Response Message: ${response.message()}")

                    // ✅ 에러 바디도 확인 (중요!)
                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string()
                        Log.e("KakaoServer", "📥 Error Body: $errorBody")
                    }

                    val responseBody = response.body()
                    Log.d("KakaoServer", "📦 Body Code: ${responseBody?.code}")
                    Log.d("KakaoServer", "📦 Body Message: ${responseBody?.message}")
                    Log.d("KakaoServer", "📦 Body Data: ${responseBody?.data}")

                    if (response.isSuccessful && responseBody != null) {
                        // ✅ C로 시작하고 성공을 나타내는 모든 코드 허용
                        if (responseBody.code.startsWith("C2") || responseBody.code == "SUCCESS" || responseBody.code == "COMMON200") {
                            val tokenResult = responseBody.data

                            if (tokenResult != null) {
                                Log.d("AUTH", "✅ 서버 accessToken = ${tokenResult.accessToken}")
                                Log.d("AUTH", "✅ 서버 refreshToken = ${tokenResult.refreshToken}")

                                // ✅ 현재 시간 확인
                                val currentTime = System.currentTimeMillis() / 1000
                                Log.d("AUTH", "⏰ Current Unix Timestamp: $currentTime")
                                Log.d("AUTH", "⏰ Current Time: ${java.util.Date()}")

                                TokenManager.saveTokens(
                                    this@IntroActivity,
                                    tokenResult.accessToken,
                                    tokenResult.refreshToken
                                )

                                saveUserInfo(tokenResult.userId, "KAKAO")

                                Toast.makeText(
                                    this@IntroActivity,
                                    "카카오 로그인 성공",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navigateToMain()
                            } else {
                                Log.d(
                                    "KakaoServer",
                                    "⚠️ Token result is null - new user, going to signup"
                                )
                                val intent = Intent(this@IntroActivity, SignUpActivity::class.java)
                                intent.putExtra("loginType", "KAKAO")
                                startActivity(intent)
                            }
                        } else {
                            Log.e("KakaoServer", "❌ Unexpected code: ${responseBody.code}")
                            Toast.makeText(
                                this@IntroActivity,
                                "로그인 실패: ${responseBody.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<BaseResponse<TokenResult>>, t: Throwable) {
                    Log.e("KakaoServer", "❌ Network failure", t)
                    Log.e("KakaoServer", "❌ Error message: ${t.message}")
                    Log.e("KakaoServer", "❌ Error cause: ${t.cause}")
                    Toast.makeText(this@IntroActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
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