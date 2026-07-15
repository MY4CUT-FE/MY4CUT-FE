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
import androidx.fragment.app.activityViewModels
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.auth.model.LoginRequest
import com.umc.mobile.my4cut.data.auth.model.SignUpRequest
import com.umc.mobile.my4cut.data.auth.model.TokenResult
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.FragmentSignUpStep3Binding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.signup.AuthViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpStep3Fragment : Fragment() {

    private var _binding: FragmentSignUpStep3Binding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

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

            // 닉네임 형식 검사: 한글/영어만 허용, 최대 7자
            if (!viewModel.isValidNickname(nickname)) {
                Toast.makeText(
                    requireContext(),
                    "한글, 영어만 입력 가능하며, 7자 이내로 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val request = SignUpRequest(
                email = emailStr,
                password = passwordStr,
                nickname = nickname
            )

            RetrofitClient.authServiceNoAuth.signUp(request)
                .enqueue(object : Callback<BaseResponse<Any>> {

                    override fun onResponse(
                        call: Call<BaseResponse<Any>>,
                        response: Response<BaseResponse<Any>>
                    ) {
                        Log.d("SignUp", "http=${response.code()}")

                        val errorBody = try {
                            response.errorBody()?.string()
                        } catch (e: Exception) {
                            null
                        }
                        Log.d("SignUp", "raw=$errorBody")
                        Log.d("SignUp", "headers=${response.headers()}")

                        val body = response.body()
                        Log.d("SignUp", "body=$body")

                        when (response.code()) {
                            200, 201 -> {
                                if (body != null && body.code.startsWith("C20")) {
                                    autoLogin()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        body?.message ?: "회원가입 실패",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            409 -> {
                                Toast.makeText(
                                    requireContext(),
                                    "이미 가입된 이메일입니다",
                                    Toast.LENGTH_LONG
                                ).show()
                                parentFragmentManager.popBackStack()
                            }
                            410 -> {
                                Toast.makeText(
                                    requireContext(),
                                    "탈퇴한 계정입니다. 고객센터에 문의해주세요.",
                                    Toast.LENGTH_LONG
                                ).show()
                                parentFragmentManager.popBackStack()
                            }
                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    body?.message ?: "회원가입 실패 (${response.code()})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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

    private fun autoLogin() {
        val loginRequest = LoginRequest(
            email = emailStr,
            password = passwordStr
        )

        RetrofitClient.authServiceNoAuth.login(loginRequest)
            .enqueue(object : Callback<BaseResponse<TokenResult>> {

                override fun onResponse(
                    call: Call<BaseResponse<TokenResult>>,
                    response: Response<BaseResponse<TokenResult>>
                ) {
                    val tokenResult = response.body()?.data

                    if (response.isSuccessful && tokenResult != null) {
                        TokenManager.saveTokens(
                            requireContext(),
                            tokenResult.accessToken,
                            tokenResult.refreshToken
                        )
                        saveUserInfo(tokenResult.userId)
                        Toast.makeText(
                            requireContext(),
                            "회원가입이 완료되었습니다!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

    private fun saveUserInfo(userId: Int) {
        requireContext()
            .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .edit().apply {
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
