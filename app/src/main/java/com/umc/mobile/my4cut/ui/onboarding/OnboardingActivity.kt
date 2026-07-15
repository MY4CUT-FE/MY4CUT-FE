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
import com.umc.mobile.my4cut.databinding.ActivityOnboardingBinding
import com.umc.mobile.my4cut.ui.login.LoginActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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