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
            false
        }
        binding.etPassword.setOnTouchListener(toggleListener)
        binding.etPasswordCheck.setOnTouchListener(toggleListener)
    }

    private fun initClickListener() {
        // 여기가 첫 화면이므로 액티비티를 종료해야 함
        binding.btnBack.setOnClickListener { requireActivity().finish() }
        // 중복 확인 버튼
        binding.btnCheckDuplicate.setOnClickListener {
            val email = binding.etEmail.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // [테스트용 가짜 로직]
            // "test@gmail.com" 이면 중복된 이메일로 간주 (실패 케이스)
            if (email == "test@gmail.com") {
                isEmailVerified = false // 인증 실패 처리

                binding.tvEmailStatus.visibility = View.VISIBLE
                binding.tvEmailStatus.text = "이미 가입된 이메일입니다."

                // 실패 스타일 (빨간색)
                val color = Color.RED
                binding.tvEmailStatus.setTextColor(color)

                // 느낌표 아이콘 + 빨간 틴트
                binding.tvEmailStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
                binding.tvEmailStatus.compoundDrawables[0].setTint(color)

                // 에러 테두리 적용
                binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)

            } else {
                // 테스트 이메일 외 모든 이메일은 성공으로 간주
                isEmailVerified = true // 인증 성공 처리

                binding.tvEmailStatus.visibility = View.VISIBLE
                binding.tvEmailStatus.text = "사용 가능한 아이디에요."

                // 성공 스타일 (연두색)
                val color = ContextCompat.getColor(requireContext(), R.color.success_green)
                binding.tvEmailStatus.setTextColor(color)

                // 체크 아이콘 + 연두색 틴트
                binding.tvEmailStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
                binding.tvEmailStatus.compoundDrawables[0].setTint(color)

                // 성공 테두리 적용
                binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_success)
            }

            // 버튼 상태 갱신
            checkInputValidity()
        }

        binding.btnNext.setOnClickListener {
            // 다음 단계(닉네임 입력)로 이동
            (activity as? SignUpActivity)?.changeFragment(SignUpStep3Fragment())
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

        // 이메일 입력 변경 시 초기화
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                isEmailVerified = false
                binding.tvEmailStatus.visibility = View.GONE
                // 배경 원래대로(회색) 복구
                binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
                checkInputValidity()
            }
        })

        binding.etPassword.addTextChangedListener(watcher)
        binding.etPasswordCheck.addTextChangedListener(watcher)
    }

    // 비밀번호 확인 로직
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
            // [일치] 성공 스타일 적용 (우측 정렬됨)
            binding.tvPwStatus.visibility = View.VISIBLE
            binding.tvPwStatus.text = "비밀번호가 일치해요."

            val color = ContextCompat.getColor(requireContext(), R.color.success_green)
            binding.tvPwStatus.setTextColor(color)

            // 체크 아이콘 + 연두색 틴트
            binding.tvPwStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
            binding.tvPwStatus.compoundDrawables[0].setTint(color)

            binding.etPasswordCheck.setBackgroundResource(R.drawable.bg_edittext_success)
            isPasswordMatched = true
        } else {
            // [불일치] 실패 스타일 적용 (우측 정렬됨)
            binding.tvPwStatus.visibility = View.VISIBLE
            binding.tvPwStatus.text = "비밀번호가 일치하지 않습니다."

            val color = Color.RED // 실패 색상
            binding.tvPwStatus.setTextColor(color)

            // 느낌표 아이콘 + 빨간색 틴트
            binding.tvPwStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
            binding.tvPwStatus.compoundDrawables[0].setTint(color)

            binding.etPasswordCheck.setBackgroundResource(R.drawable.bg_edittext_rounded) // 혹은 에러용 빨간 테두리
            isPasswordMatched = false
        }
    }

    private fun checkInputValidity() {
        val email = binding.etEmail.text.toString()
        val pw = binding.etPassword.text.toString()

        val isValid = email.isNotEmpty() &&
                isEmailVerified &&
                pw.isNotEmpty() &&
                isPasswordMatched

        binding.btnNext.isEnabled = isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}