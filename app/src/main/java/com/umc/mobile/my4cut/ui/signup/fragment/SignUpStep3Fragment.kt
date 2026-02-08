package com.umc.mobile.my4cut.ui.signup.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.auth.model.LoginRequest
import com.umc.mobile.my4cut.data.auth.model.SignUpRequest
import com.umc.mobile.my4cut.data.auth.model.TokenResult
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.FragmentSignUpStep3Binding
import com.umc.mobile.my4cut.data.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpStep3Fragment : Fragment() {

    private var _binding: FragmentSignUpStep3Binding? = null
    private val binding get() = _binding!!

    private var emailStr = ""
    private var passwordStr = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailStr = arguments?.getString("email") ?: ""
        passwordStr = arguments?.getString("password") ?: ""

        initClickListener()
        initTextWatcher()
    }

    private fun initClickListener() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnNext.setOnClickListener {
            val nickname = binding.etNickname.text.toString()

            val request = SignUpRequest(
                email = emailStr,
                password = passwordStr,
                nickname = nickname
            )

            RetrofitClient.authService.signUp(request)
                .enqueue(object : Callback<BaseResponse<Any>> {

                    override fun onResponse(
                        call: Call<BaseResponse<Any>>,
                        response: Response<BaseResponse<Any>>
                    ) {
                        Log.d("SignUp", "http=${response.code()}")
                        Log.d("SignUp", "raw=${response.errorBody()?.string()}")
                        Log.d("SignUp", "headers=${response.headers()}")

                        val body = response.body()
                        Log.d("SignUp", "body=$body")

                        if (response.isSuccessful && body != null && body.code.startsWith("C20")) {
                            autoLogin()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                body?.message ?: "회원가입 실패",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<BaseResponse<Any>>,
                        t: Throwable
                    ) {
                        Toast.makeText(
                            requireContext(),
                            "네트워크 오류",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })

        }
    }

    // 자동 로그인
    private fun autoLogin() {
        val loginRequest = LoginRequest(
            email = emailStr,
            password = passwordStr
        )

        RetrofitClient.authService.login(loginRequest)
            .enqueue(object : Callback<BaseResponse<TokenResult>> {

                override fun onResponse(
                    call: Call<BaseResponse<TokenResult>>,
                    response: Response<BaseResponse<TokenResult>>
                ) {
                    val tokenResult = response.body()?.data

                    if (response.isSuccessful && tokenResult != null) {

                        // 토큰은 TokenManager에 저장
                        TokenManager.saveTokens(
                            requireContext(),
                            tokenResult.accessToken,
                            tokenResult.refreshToken
                        )

                        // 사용자 정보만 UserPrefs
                        saveUserInfo(tokenResult.userId)

                        Toast.makeText(
                            requireContext(),
                            "회원가입 및 로그인 성공!",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)

                        requireActivity().finish()

                    } else {
                        Toast.makeText(
                            requireContext(),
                            "자동 로그인 실패",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<TokenResult>>, t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        "로그인 오류",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /** 사용자 정보 저장 (토큰 x) */
    private fun saveUserInfo(userId: Int) {
        val pref = requireContext()
            .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        pref.edit().apply {
            putInt("userId", userId)
            putString("loginType", "EMAIL")
            apply()
        }
    }

    private fun initTextWatcher() {
        binding.etNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnNext.isEnabled = s.toString().isNotEmpty()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}