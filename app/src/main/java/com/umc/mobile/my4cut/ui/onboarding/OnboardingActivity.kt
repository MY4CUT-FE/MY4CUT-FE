package com.umc.mobile.my4cut.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import com.umc.mobile.my4cut.databinding.ActivityOnboardingBinding
import com.umc.mobile.my4cut.ui.intro.IntroActivity
import com.umc.mobile.my4cut.ui.signup.SignUpActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // "터치하여 시작하기" 깜빡이기 애니메이션
        val blinkAnim = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 700
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        binding.tvStartHint.startAnimation(blinkAnim)

        // 화면 어디든 터치하면 첫 사용 여부에 따라 분기
        binding.root.setOnClickListener {
            val prefs = getSharedPreferences("my4cut_prefs", Context.MODE_PRIVATE)
            val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

            if (isFirstLaunch) {
                prefs.edit().putBoolean("is_first_launch", false).apply()
                startActivity(Intent(this, SignUpActivity::class.java))
            } else {
                startActivity(Intent(this, IntroActivity::class.java))
            }
            finish()
        }
    }
}
