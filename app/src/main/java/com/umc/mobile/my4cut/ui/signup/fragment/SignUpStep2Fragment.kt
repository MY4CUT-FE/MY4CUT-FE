package com.umc.mobile.my4cut.ui.signup.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentSignUpStep2Binding
import com.umc.mobile.my4cut.ui.signup.SignUpActivity

class SignUpStep2Fragment : Fragment() {

    private var _binding: FragmentSignUpStep2Binding? = null
    private val binding get() = _binding!!

    private var isEmailVerified = false
    private var isPasswordMatched = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClickListener()
        initTextWatchers()
        initPasswordToggle()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initPasswordToggle() {
        val toggleListener = View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val editText = v as EditText
                // 오른쪽 아이콘(눈알) 영역 클릭 감지
                if (editText.compoundDrawables[2] != null) {
                    if (event.rawX >= (editText.right - editText.compoundDrawables[2].bounds.width())) {
                        val selection = editText.selectionEnd
                        if (editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_on, 0)
                        } else {
                            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0)
                        }
                        editText.compoundDrawables[2]?.setTint(ContextCompat.getColor(requireContext(), R.color.gray_500))
                        editText.setSelection(selection)
                        return@OnTouchListener true
                    }
                }
            }
            false
        }
        binding.etPassword.setOnTouchListener(toggleListener)
        binding.etPasswordCheck.setOnTouchListener(toggleListener)
    }

    private fun initClickListener() {
        binding.btnBack.setOnClickListener { requireActivity().finish() }

        // ✅ 중복 확인 버튼 - GET 방식으로 서버 체크
        binding.btnCheckDuplicate.setOnClickListener {
            val email = binding.etEmail.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 이메일 형식 체크
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                isEmailVerified = false
                updateEmailStatusUI(false, "올바른 이메일 형식이 아닙니다.")
                checkInputValidity()
                return@setOnClickListener
            }

            // ✅ 서버에 중복 체크 (GET 방식)
            checkEmailDuplicateOnServer(email)
        }

        binding.btnNext.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            // Step3로 데이터 전달
            val fragment = SignUpStep3Fragment()
            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("password", password)
            fragment.arguments = bundle

            (activity as? SignUpActivity)?.changeFragment(fragment)
        }
    }

    /**
     * ✅ GET 방식으로 이메일 중복 체크
     * GET /auth/check-email?email={email}
     */
    private fun checkEmailDuplicateOnServer(email: String) {
        // 로딩 표시
        binding.btnCheckDuplicate.isEnabled = false
        binding.tvEmailStatus.visibility = View.VISIBLE
        binding.tvEmailStatus.text = "확인 중..."
        binding.tvEmailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))

        android.util.Log.d("SignUpStep2", "📤 중복 체크 시작 (GET): $email")

        com.umc.mobile.my4cut.network.RetrofitClient.authServiceNoAuth.checkEmailDuplicateGet(email)
            .enqueue(object : retrofit2.Callback<com.umc.mobile.my4cut.data.base.BaseResponse<com.umc.mobile.my4cut.data.auth.model.EmailCheckResponse>> {

                override fun onResponse(
                    call: retrofit2.Call<com.umc.mobile.my4cut.data.base.BaseResponse<com.umc.mobile.my4cut.data.auth.model.EmailCheckResponse>>,
                    response: retrofit2.Response<com.umc.mobile.my4cut.data.base.BaseResponse<com.umc.mobile.my4cut.data.auth.model.EmailCheckResponse>>
                ) {
                    binding.btnCheckDuplicate.isEnabled = true

                    android.util.Log.d("SignUpStep2", "📨 응답 코드: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val responseData = response.body()?.data

                        android.util.Log.d("SignUpStep2", "📨 응답 데이터: $responseData")

                        if (responseData != null) {
                            if (responseData.duplicated) {
                                // ❌ 중복된 이메일 (duplicated: true)
                                isEmailVerified = false
                                updateEmailStatusUI(false, "이미 사용 중인 이메일입니다.")
                                android.util.Log.d("SignUpStep2", "❌ 이메일 중복: ${responseData.email}")
                            } else {
                                // ✅ 사용 가능한 이메일 (duplicated: false)
                                isEmailVerified = true
                                updateEmailStatusUI(true, "사용 가능한 아이디예요.")
                                android.util.Log.d("SignUpStep2", "✅ 이메일 사용 가능: ${responseData.email}")
                            }
                        } else {
                            // 데이터가 null인 경우
                            isEmailVerified = false
                            Toast.makeText(requireContext(), "중복 확인 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                            android.util.Log.e("SignUpStep2", "⚠️ 응답 데이터가 null입니다")
                        }
                    } else {
                        // 응답 실패
                        isEmailVerified = false
                        val errorBody = try {
                            response.errorBody()?.string()
                        } catch (e: Exception) {
                            null
                        }
                        android.util.Log.e("SignUpStep2", "❌ 중복 체크 실패: ${response.code()}, $errorBody")
                        Toast.makeText(requireContext(), "중복 확인에 실패했습니다", Toast.LENGTH_SHORT).show()
                    }

                    checkInputValidity()
                }

                override fun onFailure(
                    call: retrofit2.Call<com.umc.mobile.my4cut.data.base.BaseResponse<com.umc.mobile.my4cut.data.auth.model.EmailCheckResponse>>,
                    t: Throwable
                ) {
                    binding.btnCheckDuplicate.isEnabled = true
                    isEmailVerified = false
                    android.util.Log.e("SignUpStep2", "💥 네트워크 오류", t)
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                    checkInputValidity()
                }
            })
    }

    private fun updateEmailStatusUI(isSuccess: Boolean, message: String) {
        binding.tvEmailStatus.visibility = View.VISIBLE
        binding.tvEmailStatus.text = message

        if (isSuccess) {
            val color = ContextCompat.getColor(requireContext(), R.color.success_green)
            binding.tvEmailStatus.setTextColor(color)
            binding.tvEmailStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
            binding.tvEmailStatus.compoundDrawables[0]?.setTint(color)
            binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_success)
        } else {
            val color = Color.RED
            binding.tvEmailStatus.setTextColor(color)
            binding.tvEmailStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
            binding.tvEmailStatus.compoundDrawables[0]?.setTint(color)
            binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
        }
    }

    private fun initTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkPasswordMatch()
                checkInputValidity()
            }
        }

        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 이메일 수정 시 인증 상태 초기화
                isEmailVerified = false
                binding.tvEmailStatus.visibility = View.GONE
                binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
                checkInputValidity()
            }
        })

        binding.etPassword.addTextChangedListener(watcher)
        binding.etPasswordCheck.addTextChangedListener(watcher)
    }

    private fun checkPasswordMatch() {
        val pw = binding.etPassword.text.toString()
        val pwCheck = binding.etPasswordCheck.text.toString()

        if (pwCheck.isEmpty()) {
            binding.tvPwStatus.visibility = View.GONE
            binding.etPasswordCheck.setBackgroundResource(R.drawable.bg_edittext_rounded)
            isPasswordMatched = false
            return
        }

        if (pw == pwCheck) {
            binding.tvPwStatus.visibility = View.VISIBLE
            binding.tvPwStatus.text = "비밀번호가 일치해요."
            val color = ContextCompat.getColor(requireContext(), R.color.success_green)
            binding.tvPwStatus.setTextColor(color)
            binding.tvPwStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
            binding.tvPwStatus.compoundDrawables[0]?.setTint(color)
            binding.etPasswordCheck.setBackgroundResource(R.drawable.bg_edittext_success)
            isPasswordMatched = true
        } else {
            binding.tvPwStatus.visibility = View.VISIBLE
            binding.tvPwStatus.text = "비밀번호가 일치하지 않습니다."
            val color = Color.RED
            binding.tvPwStatus.setTextColor(color)
            binding.tvPwStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
            binding.tvPwStatus.compoundDrawables[0]?.setTint(color)
            binding.etPasswordCheck.setBackgroundResource(R.drawable.bg_edittext_rounded)
            isPasswordMatched = false
        }
    }

    private fun checkInputValidity() {
        val email = binding.etEmail.text.toString()
        val pw = binding.etPassword.text.toString()

        binding.btnNext.isEnabled = email.isNotEmpty() && isEmailVerified && pw.isNotEmpty() && isPasswordMatched
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}