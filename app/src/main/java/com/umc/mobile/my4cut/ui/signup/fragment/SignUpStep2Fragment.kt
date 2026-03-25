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
            // TODO: API 연결 후 인증코드 발송 API 호출로 교체
            showSendGuide(email)
        }

        binding.btnVerify.setOnClickListener {
            val code = binding.etVerificationCode.text.toString()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "인증코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO: API 연결 후 인증코드 확인 API 호출로 교체
            showVerificationComplete()
        }

        binding.btnNext.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            val fragment = SignUpStep3Fragment()
            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("password", password)
            fragment.arguments = bundle

            (activity as? SignUpActivity)?.changeFragment(fragment)
        }
    }

    /** 인증코드 발송 후 안내 멘트 + 입력 영역 표시 */
    private fun showSendGuide(email: String) {
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
        binding.tvEmailStatus.visibility = View.VISIBLE
        binding.tvEmailStatus.text = "${email}로 인증코드를 발송했습니다.\n메일을 확인하고 인증코드 6자리를 입력해 주세요."
        binding.tvEmailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
        binding.tvEmailStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        binding.etVerificationCode.visibility = View.VISIBLE
        binding.btnVerify.visibility = View.VISIBLE
    }

    /** 이메일 에러 표시 (이미 가입된 이메일 등) */
    private fun showEmailError(message: String) {
        isEmailVerified = false
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

    /** 인증 완료 UI 처리 */
    private fun showVerificationComplete() {
        isEmailVerified = true
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_success)
        binding.etVerificationCode.setBackgroundResource(R.drawable.bg_edittext_success)
        binding.tvVerificationStatus.visibility = View.VISIBLE
        binding.tvVerificationStatus.text = "인증이 완료되었습니다."
        val color = ContextCompat.getColor(requireContext(), R.color.success_green)
        binding.tvVerificationStatus.setTextColor(color)
        binding.tvVerificationStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
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
