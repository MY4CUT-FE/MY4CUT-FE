package com.umc.mobile.my4cut.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.auth.model.TokenResult
import com.umc.mobile.my4cut.databinding.ActivityOnboardingBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.login.LoginActivity
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        when {
            // 유효한 access token이 있으면 온보딩/로그인 건너뛰고 바로 메인으로 이동
            TokenManager.isAccessTokenValid(this) -> proceedToMain()

            // access token은 만료됐지만 refresh token이 있으면 갱신을 먼저 시도
            !TokenManager.getRefreshToken(this).isNullOrEmpty() -> tryRefreshThenProceed()

            // refresh token도 없으면 온보딩 화면 표시
            else -> showOnboarding()
        }
    }

    private fun tryRefreshThenProceed() {
        val refreshToken = TokenManager.getRefreshToken(this) ?: return showOnboarding()

        RetrofitClient.authServiceNoAuth.refresh("Bearer $refreshToken")
            .enqueue(object : Callback<BaseResponse<TokenResult>> {
                override fun onResponse(
                    call: Call<BaseResponse<TokenResult>>,
                    response: Response<BaseResponse<TokenResult>>
                ) {
                    val newTokens = response.body()?.data
                    if (response.isSuccessful && newTokens != null) {
                        TokenManager.saveTokens(
                            this@OnboardingActivity,
                            newTokens.accessToken,
                            newTokens.refreshToken
                        )
                        proceedToMain()
                    } else {
                        showOnboarding()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<TokenResult>>, t: Throwable) {
                    // 네트워크 오류 등으로 갱신에 실패한 경우에만 로그인 화면으로 이동
                    showOnboarding()
                }
            })
    }

    private fun proceedToMain() {
        val openedFromNotification =
            intent.hasExtra("type") ||
                    intent.hasExtra("notificationId") ||
                    intent.hasExtra("google.message_id")

        val destinationIntent = if (openedFromNotification) {
            when (intent.getStringExtra("type")) {
                "WORKSPACE_INVITE",
                "FRIEND_REQUEST" -> {
                    Intent(this, NotificationActivity::class.java)
                }

                "PHOTO_COMMENT" -> {
                    Intent(this, MainActivity::class.java)
                }

                else -> {
                    Intent(this, NotificationActivity::class.java)
                }
            }.apply {
                intent.extras?.let(::putExtras)
            }
        } else {
            Intent(this, MainActivity::class.java)
        }

        destinationIntent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(destinationIntent)
        finish()
    }

    private fun showOnboarding() {
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()

        // "터치하여 시작하기" 깜빡이기 애니메이션
        val blinkAnim = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 700
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        binding.tvStartHint.startAnimation(blinkAnim)

        // 화면 어디든 터치하면 로그인 화면으로 이동
        binding.root.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }
}