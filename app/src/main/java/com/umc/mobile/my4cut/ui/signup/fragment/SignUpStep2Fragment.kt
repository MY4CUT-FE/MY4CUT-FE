package com.umc.mobile.my4cut.ui.signup.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.FragmentSignUpStep2Binding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.signup.AuthViewModel
import com.umc.mobile.my4cut.ui.signup.SignUpActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpStep2Fragment : Fragment() {

    private var _binding: FragmentSignUpStep2Binding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

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

        // 인증코드 발송 버튼
        binding.btnSendVerification.setOnClickListener {
            val email = binding.etEmail.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showEmailError("올바른 이메일 형식이 아닙니다.")
                return@setOnClickListener
            }
            sendVerificationCode(email)
        }

        // 인증 버튼
        binding.btnVerify.setOnClickListener {
            val code = binding.etVerificationCode.text.toString()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "인증코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val email = binding.etEmail.text.toString()
            verifyCode(email, code)
        }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (!viewModel.isValidPassword(password)) {
                Toast.makeText(
                    requireContext(),
                    "영어/숫자/특수기호만 입력 가능하며, 8~15자로 비밀번호를 설정해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val fragment = SignUpStep3Fragment()
            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("password", password)
            fragment.arguments = bundle
            (activity as? SignUpActivity)?.changeFragment(fragment)
        }
    }

    /** POST /auth/email/send - 이메일 인증코드 발송 */
    private fun sendVerificationCode(email: String) {
        binding.btnSendVerification.isEnabled = false

        RetrofitClient.authServiceNoAuth.sendEmailVerificationCode(mapOf("email" to email))
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    binding.btnSendVerification.isEnabled = true

                    // 서버 응답 전체를 Logcat에 출력 (디버깅용)
                    val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    Log.d("SignUp_Send", "http=${response.code()}")
                    Log.d("SignUp_Send", "body=${response.body()}")
                    Log.d("SignUp_Send", "errorBody=$errorBody")

                    if (response.isSuccessful) {
                        showSendGuide(email)
                    } else {
                        val message = when (response.code()) {
                            400 -> "올바른 이메일 형식이 아닙니다."
                            409 -> "이미 가입된 이메일입니다."
                            500 -> "서버 오류가 발생했습니다.\nLogcat > SignUp_Send 태그를 확인해주세요."
                            else -> "인증코드 발송에 실패했습니다. (${response.code()})"
                        }
                        showEmailError(message)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    binding.btnSendVerification.isEnabled = true
                    Log.e("SignUp_Send", "failure", t)
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** POST /auth/email/verify - 인증코드 검증 */
    private fun verifyCode(email: String, code: String) {
        binding.btnVerify.isEnabled = false

        val request = mapOf("email" to email, "code" to code)
        RetrofitClient.authServiceNoAuth.verifyEmailCode(request)
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    binding.btnVerify.isEnabled = true
                    Log.d("SignUp", "verifyCode http=${response.code()}, body=${response.body()}")

                    if (response.isSuccessful) {
                        showVerificationComplete()
                    } else {
                        val errorCode = response.code()
                        val message = when (errorCode) {
                            400 -> "인증코드가 올바르지 않습니다."
                            410 -> "인증코드가 만료되었습니다. 다시 발송해주세요."
                            else -> "인증에 실패했습니다. (${errorCode})"
                        }
                        showVerificationError(message)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    binding.btnVerify.isEnabled = true
                    Log.e("SignUp", "verifyCode failure", t)
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** 인증코드 발송 후 안내 문구 + 입력 영역 표시 */
    private fun showSendGuide(email: String) {
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
        binding.tvEmailStatus.visibility = View.VISIBLE
        binding.tvEmailStatus.text = "${email}로 인증 코드를 발송했습니다.\n메일을 확인하고 인증코드 6자리를 입력해 주세요."
        binding.tvEmailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
        binding.tvEmailStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        binding.etVerificationCode.visibility = View.VISIBLE
        binding.btnVerify.visibility = View.VISIBLE
        // 재발송 시 인증 상태 초기화
        binding.tvVerificationStatus.visibility = View.GONE
        binding.etVerificationCode.setBackgroundResource(R.drawable.bg_edittext_rounded)
        isEmailVerified = false
        viewModel.setEmailVerified(false)
        checkInputValidity()
    }

    /** 이메일 에러 표시 */
    private fun showEmailError(message: String) {
        isEmailVerified = false
        viewModel.setEmailVerified(false)
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_error)
        binding.tvEmailStatus.visibility = View.VISIBLE
        binding.tvEmailStatus.text = message
        val color = ContextCompat.getColor(requireContext(), R.color.modal_red)
        binding.tvEmailStatus.setTextColor(color)
        binding.tvEmailStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
        binding.tvEmailStatus.compoundDrawables[0]?.setTint(color)
        binding.etVerificationCode.visibility = View.GONE
        binding.btnVerify.visibility = View.GONE
        binding.tvVerificationStatus.visibility = View.GONE
        checkInputValidity()
    }

    /** 인증 완료 UI */
    private fun showVerificationComplete() {
        isEmailVerified = true
        viewModel.setEmailVerified(true)
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_success)
        binding.etVerificationCode.setBackgroundResource(R.drawable.bg_edittext_success)
        binding.tvVerificationStatus.visibility = View.VISIBLE
        binding.tvVerificationStatus.text = "인증이 완료되었어요."
        val color = ContextCompat.getColor(requireContext(), R.color.success_green)
        binding.tvVerificationStatus.setTextColor(color)
        binding.tvVerificationStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
        binding.tvVerificationStatus.compoundDrawables[0]?.setTint(color)
        checkInputValidity()
    }

    /** 인증 실패 UI */
    private fun showVerificationError(message: String) {
        isEmailVerified = false
        viewModel.setEmailVerified(false)
        binding.etVerificationCode.setBackgroundResource(R.drawable.bg_edittext_error)
        binding.tvVerificationStatus.visibility = View.VISIBLE
        binding.tvVerificationStatus.text = message
        val color = ContextCompat.getColor(requireContext(), R.color.modal_red)
        binding.tvVerificationStatus.setTextColor(color)
        binding.tvVerificationStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
        binding.tvVerificationStatus.compoundDrawables[0]?.setTint(color)
        checkInputValidity()
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
                // 이메일 수정 시 인증 상태 전체 초기화
                isEmailVerified = false
                viewModel.setEmailVerified(false)
                binding.tvEmailStatus.visibility = View.GONE
                binding.etVerificationCode.visibility = View.GONE
                binding.btnVerify.visibility = View.GONE
                binding.tvVerificationStatus.visibility = View.GONE
                binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
                binding.etVerificationCode.setBackgroundResource(R.drawable.bg_edittext_rounded)
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
            binding.tvPwStatus.text = "비밀번호가 일치합니다."
            val color = ContextCompat.getColor(requireContext(), R.color.success_green)
            binding.tvPwStatus.setTextColor(color)
            binding.tvPwStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
            binding.tvPwStatus.compoundDrawables[0]?.setTint(color)
            binding.etPasswordCheck.setBackgroundResource(R.drawable.bg_edittext_success)
            isPasswordMatched = true
        } else {
            binding.tvPwStatus.visibility = View.VISIBLE
            binding.tvPwStatus.text = "비밀번호가 일치하지 않습니다."
            val color = ContextCompat.getColor(requireContext(), R.color.modal_red)
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
