package com.umc.mobile.my4cut.ui.signup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ActivitySignUpBinding
// [수정] Step1 삭제, Step2 import
import com.umc.mobile.my4cut.ui.signup.fragment.SignUpStep2Fragment

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 화면 -> 이메일 입력 화면
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fcv_signup, SignUpStep2Fragment())
                .commitAllowingStateLoss()
        }
    }

    // 프래그먼트 변경 함수
    fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
//            .setCustomAnimations(
//                R.anim.slide_in_right,
//                R.anim.slide_out_left,
//                R.anim.slide_in_left,
//                R.anim.slide_out_right
//            )
            .replace(R.id.fcv_signup, fragment)
            .addToBackStack(null)
            .commit()
    }
}