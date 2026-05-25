package com.umc.mobile.my4cut.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.auth.model.KakaoLoginRequest
import com.umc.mobile.my4cut.data.auth.model.LoginRequest
import com.umc.mobile.my4cut.data.auth.model.TokenResult
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.notification.model.RegisterTokenRequestDto
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.databinding.ActivityLoginBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.signup.SignUpActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    companion object {
        private const val PREFS_NAME = "my4cut_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }

    private val fcmRegisterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initClickListener()
        initTextWatchers()
        initPasswordToggle()
        initUnderlineLinks()
        checkInputValidity()
    }

    private fun initClickListener() {
        // [수정] 뒤로가기 버튼 삭제로 클릭 리스너 제거

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.btnKakaoLogin.setOnClickListener {
            startKakaoLogin()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            RetrofitClient.authServiceNoAuth.login(LoginRequest(email = email, password = password))
                .enqueue(object : Callback<BaseResponse<TokenResult>> {

                    override fun onResponse(
                        call: Call<BaseResponse<TokenResult>>,
                        response: Response<BaseResponse<TokenResult>>
                    ) {
                        val resp = response.body()
                        Log.d("Login", "http=${response.code()}, code=${resp?.code}")

                        when (response.code()) {
                            200, 201 -> {
                                if (resp != null && resp.code.startsWith("C20")) {
                                    val tokenResult = resp.data
                                    if (tokenResult != null) {
                                        TokenManager.saveTokens(
                                            this@LoginActivity,
                                            tokenResult.accessToken,
                                            tokenResult.refreshToken
                                        )
                                        checkIfAccountIsActive()
                                    } else {
                                        Toast.makeText(this@LoginActivity, "토큰 정보를 받아오지 못했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            401 -> {
                                val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                                if (errorBody?.contains("탈퇴") == true || resp?.code == "C4011") {
                                    Toast.makeText(this@LoginActivity, "탈퇴한 계정입니다. 로그인할 수 없습니다.", Toast.LENGTH_LONG).show()
                                } else {
                                    showLoginError()
                                }
                            }
                            404 -> showLoginError()
                            else -> Toast.makeText(this@LoginActivity, resp?.message ?: "로그인 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<BaseResponse<TokenResult>>, t: Throwable) {
                        Log.e("LoginActivity", "Login Error", t)
                        Toast.makeText(this@LoginActivity, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun showLoginError() {
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_error)
        binding.etPassword.setBackgroundResource(R.drawable.bg_edittext_error)
        binding.tvEmailError.visibility = View.VISIBLE
        binding.tvEmailError.text = "아이디 또는 비밀번호가 일치하지 않습니다."
        binding.tvPwError.visibility = View.GONE
        // [수정] 로그인 실패 시 토스트 메시지 표시
        Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
    }

    private fun checkIfAccountIsActive() {
        RetrofitClient.userService.getMyPage()
            .enqueue(object : Callback<BaseResponse<UserMeResponse>> {
                override fun onResponse(
                    call: Call<BaseResponse<UserMeResponse>>,
                    response: Response<BaseResponse<UserMeResponse>>
                ) {
                    when (response.code()) {
                        200 -> {
                            val userData = response.body()?.data
                            if (userData != null) {
                                Toast.makeText(this@LoginActivity, "로그인에 성공했습니다.", Toast.LENGTH_SHORT).show()
                                registerFcmTokenAfterLogin()
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                handleWithdrawnAccount()
                            }
                        }
                        else -> handleWithdrawnAccount()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<UserMeResponse>>, t: Throwable) {
                    Log.e("Login", "User info check failed", t)
                    handleWithdrawnAccount()
                }
            })
    }

    private fun handleWithdrawnAccount() {
        TokenManager.clear(this@LoginActivity)
        Toast.makeText(this@LoginActivity, "탈퇴한 계정입니다. 로그인할 수 없습니다.", Toast.LENGTH_LONG).show()
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
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) return@loginWithKakaoTalk
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
        RetrofitClient.authServiceNoAuth.loginKakao(KakaoLoginRequest(accessToken = kakaoAccessToken))
            .enqueue(object : Callback<BaseResponse<TokenResult>> {
                override fun onResponse(
                    call: Call<BaseResponse<TokenResult>>,
                    response: Response<BaseResponse<TokenResult>>
                ) {
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null) {
                        if (responseBody.code.startsWith("C2") || responseBody.code == "SUCCESS" || responseBody.code == "COMMON200") {
                            val tokenResult = responseBody.data
                            if (tokenResult != null) {
                                TokenManager.saveTokens(this@LoginActivity, tokenResult.accessToken, tokenResult.refreshToken)
                                saveUserInfo(tokenResult.userId, "KAKAO")
                                Toast.makeText(this@LoginActivity, "카카오 로그인 성공", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
                                intent.putExtra("loginType", "KAKAO")
                                startActivity(intent)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<BaseResponse<TokenResult>>, t: Throwable) {
                    Log.e("KakaoLogin", "Network failure", t)
                    Toast.makeText(this@LoginActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun saveUserInfo(userId: Int, loginType: String) {
        getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
            .putInt("userId", userId)
            .putString("loginType", loginType)
            .apply()
    }

    private fun registerFcmTokenAfterLogin() {
        val savedToken = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_FCM_TOKEN, null)
        if (!savedToken.isNullOrBlank()) {
            sendFcmTokenToServer(savedToken)
            return
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (!token.isNullOrBlank()) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_FCM_TOKEN, token).apply()
                sendFcmTokenToServer(token)
            }
        }.addOnFailureListener { e ->
            Log.e("FCM", "FCM 토큰 가져오기 실패", e)
        }
    }

    private fun sendFcmTokenToServer(token: String) {
        fcmRegisterScope.launch {
            try {
                RetrofitClient.notificationService.registerToken(RegisterTokenRequestDto(fcmToken = token, device = "ANDROID"))
                Log.d("FCM", "FCM 토큰 서버 등록 성공")
            } catch (e: CancellationException) {
                Log.w("FCM", "FCM 등록 취소", e)
            } catch (e: Exception) {
                Log.e("FCM", "FCM 토큰 서버 등록 실패", e)
            }
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
                if (editText.compoundDrawables[2] != null &&
                    event.rawX >= (editText.right - editText.compoundDrawables[2].bounds.width())) {
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

    private fun initUnderlineLinks() {
        binding.btnSignup.paintFlags = binding.btnSignup.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.btnForgotPassword.paintFlags = binding.btnForgotPassword.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    override fun onDestroy() {
        super.onDestroy()
        fcmRegisterScope.cancel()
    }
}
