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
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
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

        // ✅ 카카오 로그인 키 해시 확인 (개발 중에만 사용, 배포 시 제거)
        printKeyHash()

        initClickListener()
        initTextWatchers()
        initPasswordToggle()
    }

    /**
     * 🔑 카카오 로그인을 위한 키 해시 출력
     * 로그캣에서 "KeyHash" 필터로 확인 후 카카오 개발자 콘솔에 등록
     */
    private fun printKeyHash() {
        try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )

            for (signature in packageInfo.signatures) {
                val md = java.security.MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = android.util.Base64.encodeToString(
                    md.digest(),
                    android.util.Base64.NO_WRAP
                )

                Log.d("KeyHash", "========================================")
                Log.d("KeyHash", "🔑 카카오 키 해시:")
                Log.d("KeyHash", keyHash)
                Log.d("KeyHash", "========================================")
                Log.d("KeyHash", "위 키 해시를 카카오 개발자 콘솔에 등록하세요:")
                Log.d("KeyHash", "1. https://developers.kakao.com 접속")
                Log.d("KeyHash", "2. 내 애플리케이션 → 앱 설정 → 플랫폼 → Android")
                Log.d("KeyHash", "3. 키 해시 입력란에 위 값 붙여넣기")
                Log.d("KeyHash", "4. 저장 버튼 클릭")
                Log.d("KeyHash", "========================================")
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "❌ 키 해시 확인 실패", e)
        }
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

                        when (response.code()) {
                            200, 201 -> {
                                // ✅ 로그인 성공
                                if (resp != null && resp.code.startsWith("C20")) {
                                    val tokenResult = resp.data

                                    if (tokenResult != null) {
                                        // 토큰 저장
                                        TokenManager.saveTokens(
                                            this@LoginActivity,
                                            tokenResult.accessToken,
                                            tokenResult.refreshToken
                                        )

                                        Log.d("Login", "✅ Tokens saved successfully")

                                        // ✅ 탈퇴 계정 체크: 사용자 정보 조회 시도
                                        checkIfAccountIsActive()
                                    } else {
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "토큰 정보를 받아오지 못했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            401 -> {
                                // ❌ 인증 실패 (탈퇴 계정 또는 잘못된 비밀번호)
                                val errorBody = try {
                                    response.errorBody()?.string()
                                } catch (e: Exception) {
                                    null
                                }

                                Log.d("Login", "401 Error Body: $errorBody")

                                // 탈퇴 계정 여부 확인
                                if (errorBody?.contains("탈퇴") == true || resp?.code == "C4011") {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "탈퇴한 계정입니다. 로그인할 수 없습니다.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "이메일 또는 비밀번호가 일치하지 않습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            404 -> {
                                // ❌ 가입되지 않은 계정
                                Toast.makeText(
                                    this@LoginActivity,
                                    "가입되지 않은 계정입니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                // 기타 오류
                                Toast.makeText(
                                    this@LoginActivity,
                                    resp?.message ?: "로그인 실패",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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

    /**
     * ✅ 탈퇴 계정 체크: 로그인 성공 후 사용자 정보 조회
     * 백엔드에서 탈퇴 계정도 로그인 성공 처리하므로 추가 검증 필요
     */
    private fun checkIfAccountIsActive() {
        RetrofitClient.userService.getMyPage()
            .enqueue(object : Callback<BaseResponse<UserMeResponse>> {

                override fun onResponse(
                    call: Call<BaseResponse<UserMeResponse>>,
                    response: Response<BaseResponse<UserMeResponse>>
                ) {
                    Log.d("Login", "📋 User info check: ${response.code()}")

                    when (response.code()) {
                        200 -> {
                            // ✅ 사용자 정보 조회 성공 = 정상 계정
                            val userData = response.body()?.data

                            if (userData != null) {
                                Log.d("Login", "✅ Account is active: ${userData.nickname}")

                                Toast.makeText(
                                    this@LoginActivity,
                                    "로그인에 성공했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // 메인 화면으로 이동
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                // 데이터가 없는 경우
                                handleWithdrawnAccount()
                            }
                        }
                        401, 403, 404 -> {
                            // ❌ 탈퇴 계정 또는 유효하지 않은 계정
                            handleWithdrawnAccount()
                        }
                        else -> {
                            // 기타 오류
                            Log.e("Login", "⚠️ Unexpected response: ${response.code()}")
                            handleWithdrawnAccount()
                        }
                    }
                }

                override fun onFailure(
                    call: Call<BaseResponse<UserMeResponse>>,
                    t: Throwable
                ) {
                    Log.e("Login", "❌ User info check failed", t)
                    // 네트워크 오류 시에도 탈퇴 계정으로 처리
                    handleWithdrawnAccount()
                }
            })
    }

    /**
     * 탈퇴 계정 처리: 토큰 삭제 및 로그인 화면 유지
     */
    private fun handleWithdrawnAccount() {
        // 저장된 토큰 삭제
        TokenManager.clear(this@LoginActivity)

        Log.d("Login", "❌ Withdrawn account detected - tokens cleared")

        Toast.makeText(
            this@LoginActivity,
            "탈퇴한 계정입니다. 로그인할 수 없습니다.",
            Toast.LENGTH_LONG
        ).show()
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