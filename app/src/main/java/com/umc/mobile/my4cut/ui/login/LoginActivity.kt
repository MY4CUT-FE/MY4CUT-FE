package com.umc.mobile.my4cut.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initClickListener()
        initTextWatchers()
        initPasswordToggle()
    }

    private fun initClickListener() {
        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 로그인 버튼
        binding.btnLogin.setOnClickListener {
            // TODO: 실제 서버 로그인 API 연동
            // 여기서는 임시로 메인화면으로 이동하도록 구현
            val intent = Intent(this, MainActivity::class.java)
            // 로그인 후 뒤로가기 시 로그인 화면이 안 나오게 하려면 플래그 설정
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun initTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkInputValidity()

                // 입력 중일 때는 에러 메시지 숨기기
                binding.tvEmailError.visibility = View.GONE
                binding.tvPwError.visibility = View.GONE
                binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
                binding.etPassword.setBackgroundResource(R.drawable.bg_edittext_rounded)
            }
        }

        binding.etEmail.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)
    }

    // 이메일, 비밀번호가 모두 입력되어야 버튼 활성화
    private fun checkInputValidity() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

        binding.btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty()
    }

    // 비밀번호 눈알 아이콘 토글 로직 (회원가입과 동일)
    @SuppressLint("ClickableViewAccessibility")
    private fun initPasswordToggle() {
        binding.etPassword.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val editText = v as EditText
                if (event.rawX >= (editText.right - editText.compoundDrawables[2].bounds.width())) {
                    val selection = editText.selectionEnd
                    if (editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_on, 0)
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0)
                    }
                    editText.compoundDrawables[2]?.setTint(ContextCompat.getColor(this, R.color.gray_500))
                    editText.setSelection(selection)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}