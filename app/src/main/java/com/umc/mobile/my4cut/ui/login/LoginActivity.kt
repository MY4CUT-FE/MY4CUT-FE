package com.umc.mobile.my4cut.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.auth.model.LoginRequest
import com.umc.mobile.my4cut.data.auth.model.TokenResult
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.ActivityLoginBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            val request = LoginRequest(
                email = email,
                password = password
            )

            // ✅ authServiceNoAuth 사용 (인증 없는 클라이언트)
            RetrofitClient.authServiceNoAuth.login(request)
                .enqueue(object : Callback<BaseResponse<TokenResult>> {

                    override fun onResponse(
                        call: Call<BaseResponse<TokenResult>>,
                        response: Response<BaseResponse<TokenResult>>
                    ) {
                        val resp = response.body()

                        Log.d("Login", "http=${response.code()}")
                        Log.d("Login", "code=${resp?.code}, message=${resp?.message}")

                        if (response.isSuccessful && resp != null && resp.code.startsWith("C20")) {
                            val tokenResult = resp.data

                            if (tokenResult != null) {
                                // ✅ 토큰 저장
                                TokenManager.saveTokens(
                                    this@LoginActivity,
                                    tokenResult.accessToken,
                                    tokenResult.refreshToken
                                )

                                Log.d("Login", "✅ Tokens saved successfully")

                                Toast.makeText(
                                    this@LoginActivity,
                                    "로그인에 성공했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "토큰 정보를 받아오지 못했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                resp?.message ?: "로그인 실패",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<BaseResponse<TokenResult>>,
                        t: Throwable
                    ) {
                        Log.e("LoginActivity", "Login Error", t)
                        Toast.makeText(
                            this@LoginActivity,
                            "네트워크 연결 상태를 확인해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun initTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkInputValidity()

                binding.tvEmailError.visibility = View.GONE
                binding.tvPwError.visibility = View.GONE
                binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
                binding.etPassword.setBackgroundResource(R.drawable.bg_edittext_rounded)
            }
        }

        binding.etEmail.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)
    }

    private fun checkInputValidity() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

        binding.btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty()
    }

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