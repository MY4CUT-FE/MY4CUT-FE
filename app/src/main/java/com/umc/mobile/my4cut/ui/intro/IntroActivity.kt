package com.umc.mobile.my4cut.ui.intro

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.umc.mobile.my4cut.databinding.ActivityIntroBinding
import com.umc.mobile.my4cut.ui.login.LoginActivity
import com.umc.mobile.my4cut.ui.signup.SignUpActivity

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 회원가입 버튼 밑줄
        binding.tvSignup.paintFlags = binding.tvSignup.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        initClickListener()
    }

    private fun initClickListener() {
        // [회원가입] 버튼 클릭 시 SignUpActivity로 이동
        binding.tvSignup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        //  마이포컷 로그인 버튼 클릭 -> LoginActivity 이동
        binding.btnLoginMy4cut.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // 카카오 로그인 버튼
        binding.btnLoginKakao.setOnClickListener {
            // 추후 카카오 로그인 로직 구현
        }
    }
}