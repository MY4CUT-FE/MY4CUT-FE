package com.umc.mobile.my4cut.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.auth.model.PasswordResetRequest
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.ActivityForgotPasswordBinding
import com.umc.mobile.my4cut.databinding.DialogChangePasswordBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: PasswordFindViewModel by viewModels()

    // 인증코드 발송된 이메일 (인증 완료 후 비밀번호 재설정 시 사용)
    private var sentEmail: String = ""
    // 검증 완료된 인증코드 (비밀번호 재설정 API 호출 시 함께 전달)
    private var verifiedCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initClickListeners()
    }

    private fun initClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // 인증코드 발송 버튼
        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showEmailError("이메일을 입력해주세요.")
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showEmailError("올바른 이메일 형식이 아닙니다.")
                return@setOnClickListener
            }
            sendVerificationCode(email)
        }

        // 확인 버튼: 인증코드 검증 후 비밀번호 변경 BottomSheet 표시
        binding.btnConfirm.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.isEmpty()) {
                showCodeError("인증코드를 입력해주세요.")
                return@setOnClickListener
            }
            verifyCode(sentEmail, code)
        }
    }

    /** POST /auth/email/send - 비밀번호 재설정 인증코드 발송 (인증 불필요 엔드포인트 사용) */
    private fun sendVerificationCode(email: String) {
        binding.btnSendCode.isEnabled = false
        hideEmailError()

        RetrofitClient.authServiceNoAuth.sendEmailVerificationCode(mapOf("email" to email))
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    binding.btnSendCode.isEnabled = true
                    Log.d("ForgotPw_Send", "http=${response.code()}, body=${response.body()}")

                    when (response.code()) {
                        200, 201 -> {
                            sentEmail = email
                            viewModel.setEmailVerified(false)
                            verifiedCode = ""
                            showSendGuide(email)
                        }
                        404 -> showEmailError("존재하지 않는 아이디입니다.")
                        409 -> {
                            // 이미 가입된 이메일 = 비밀번호 찾기 대상 이메일이 맞음 → 발송 안내
                            sentEmail = email
                            viewModel.setEmailVerified(false)
                            verifiedCode = ""
                            showSendGuide(email)
                        }
                        else -> {
                            val msg = response.body()?.message ?: "인증코드 발송에 실패했습니다."
                            showEmailError(msg)
                        }
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    binding.btnSendCode.isEnabled = true
                    Log.e("ForgotPw_Send", "failure", t)
                    Toast.makeText(this@ForgotPasswordActivity, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** POST /auth/email/verify - 인증코드 검증 (회원가입 로직 재사용) */
    private fun verifyCode(email: String, code: String) {
        if (email.isEmpty()) {
            showEmailError("먼저 이메일을 입력하고 인증코드를 발송해주세요.")
            return
        }
        hideCodeError()
        binding.btnConfirm.isEnabled = false

        RetrofitClient.authServiceNoAuth.verifyEmailCode(mapOf("email" to email, "code" to code))
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    binding.btnConfirm.isEnabled = true
                    Log.d("ForgotPw_Verify", "http=${response.code()}, body=${response.body()}")

                    if (response.isSuccessful) {
                        // 인증 성공 → ViewModel 상태 업데이트 후 BottomSheet 표시
                        verifiedCode = code
                        viewModel.setEmailVerified(true, email)
                        binding.etCode.setBackgroundResource(R.drawable.bg_edittext_success)
                        showChangePasswordBottomSheet(email, code)
                    } else {
                        val message = when (response.code()) {
                            400 -> "인증코드가 일치하지 않습니다."
                            410 -> "인증코드가 만료되었습니다. 다시 발송해주세요."
                            else -> "인증에 실패했습니다. (${response.code()})"
                        }
                        showCodeError(message)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    binding.btnConfirm.isEnabled = true
                    Log.e("ForgotPw_Verify", "failure", t)
                    Toast.makeText(this@ForgotPasswordActivity, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** [수정] Dialog → BottomSheetDialog로 교체 */
    private fun showChangePasswordBottomSheet(email: String, code: String) {
        val bottomSheet = BottomSheetDialog(this)
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)
        bottomSheet.setCanceledOnTouchOutside(false)

        setupPasswordToggle(dialogBinding.etNewPassword)
        setupPasswordToggle(dialogBinding.etConfirmPassword)

        dialogBinding.btnClose.setOnClickListener { bottomSheet.dismiss() }

        dialogBinding.btnChange.setOnClickListener {
            val newPassword = dialogBinding.etNewPassword.text.toString()
            val confirmPassword = dialogBinding.etConfirmPassword.text.toString()

            // 에러 초기화
            clearDialogErrors(dialogBinding)

            // 새 비밀번호 비어있는지 확인
            if (newPassword.isEmpty()) {
                showDialogError(dialogBinding.tvNewPasswordError, dialogBinding.etNewPassword, "새 비밀번호를 입력해주세요.")
                return@setOnClickListener
            }

            // 비밀번호 형식 검사: 영어/숫자/특수기호 8~15자
            if (!viewModel.isValidPassword(newPassword)) {
                showDialogError(dialogBinding.tvNewPasswordError, dialogBinding.etNewPassword, "영어/숫자/특수기호 포함 8~15자로 작성해주세요.")
                return@setOnClickListener
            }

            // 재입력 비어있는지 확인
            if (confirmPassword.isEmpty()) {
                showDialogError(dialogBinding.tvConfirmPasswordError, dialogBinding.etConfirmPassword, "비밀번호를 재입력해주세요.")
                return@setOnClickListener
            }

            // 비밀번호 일치 여부 확인
            if (newPassword != confirmPassword) {
                showDialogError(dialogBinding.tvConfirmPasswordError, dialogBinding.etConfirmPassword, "비밀번호가 일치하지 않습니다.")
                return@setOnClickListener
            }

            resetPassword(email, code, newPassword, bottomSheet, dialogBinding)
        }

        bottomSheet.show()
    }

    /** POST /auth/password/reset - 비밀번호 재설정 */
    private fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        bottomSheet: BottomSheetDialog,
        dialogBinding: DialogChangePasswordBinding
    ) {
        dialogBinding.btnChange.isEnabled = false

        RetrofitClient.authServiceNoAuth.resetPassword(
            PasswordResetRequest(email = email, code = code, newPassword = newPassword)
        ).enqueue(object : Callback<BaseResponse<Any>> {
            override fun onResponse(
                call: Call<BaseResponse<Any>>,
                response: Response<BaseResponse<Any>>
            ) {
                dialogBinding.btnChange.isEnabled = true
                Log.d("ForgotPw_Reset", "http=${response.code()}, body=${response.body()}")

                when (response.code()) {
                    200, 201 -> {
                        bottomSheet.dismiss()
                        Toast.makeText(this@ForgotPasswordActivity, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    400 -> {
                        // 인증코드 불일치: BottomSheet 닫고 메인 화면에 코드 에러 표시
                        bottomSheet.dismiss()
                        showCodeError("인증코드가 일치하지 않습니다.")
                    }
                    409 -> {
                        // 이전과 동일한 비밀번호
                        showDialogError(dialogBinding.tvNewPasswordError, dialogBinding.etNewPassword, "이전과 동일한 비밀번호입니다.")
                    }
                    else -> {
                        val msg = response.body()?.message ?: "비밀번호 변경에 실패했습니다."
                        Toast.makeText(this@ForgotPasswordActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<BaseResponse<Any>>, t: Throwable) {
                dialogBinding.btnChange.isEnabled = true
                Toast.makeText(this@ForgotPasswordActivity, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ───────────────────── UI 헬퍼 ─────────────────────

    /** 이메일 에러 표시 (빨간 테두리 + 아이콘 + 메시지) */
    private fun showEmailError(message: String) {
        binding.tvEmailError.text = message
        binding.tvEmailError.visibility = View.VISIBLE
        binding.tvEmailError.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
        val color = ContextCompat.getColor(this, R.color.modal_red)
        binding.tvEmailError.compoundDrawables[0]?.setTint(color)
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_error)
    }

    private fun hideEmailError() {
        binding.tvEmailError.visibility = View.GONE
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
    }

    /** 인증코드 에러 표시 */
    private fun showCodeError(message: String) {
        binding.tvCodeError.text = message
        binding.tvCodeError.visibility = View.VISIBLE
        binding.tvCodeError.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
        val color = ContextCompat.getColor(this, R.color.modal_red)
        binding.tvCodeError.compoundDrawables[0]?.setTint(color)
        binding.etCode.setBackgroundResource(R.drawable.bg_edittext_error)
    }

    private fun hideCodeError() {
        binding.tvCodeError.visibility = View.GONE
        binding.etCode.setBackgroundResource(R.drawable.bg_edittext_rounded)
    }

    /** BottomSheet 내 에러 표시 */
    private fun showDialogError(errorView: android.widget.TextView, inputView: EditText, message: String) {
        errorView.text = message
        errorView.visibility = View.VISIBLE
        errorView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_circle, 0, 0, 0)
        val color = ContextCompat.getColor(this, R.color.modal_red)
        errorView.compoundDrawables[0]?.setTint(color)
        inputView.setBackgroundResource(R.drawable.bg_edittext_error)
    }

    /** BottomSheet 에러 전체 초기화 */
    private fun clearDialogErrors(dialogBinding: DialogChangePasswordBinding) {
        dialogBinding.tvNewPasswordError.visibility = View.GONE
        dialogBinding.tvConfirmPasswordError.visibility = View.GONE
        dialogBinding.etNewPassword.setBackgroundResource(R.drawable.bg_edittext_rounded)
        dialogBinding.etConfirmPassword.setBackgroundResource(R.drawable.bg_edittext_rounded)
    }

    /** 인증코드 발송 성공 안내 */
    private fun showSendGuide(email: String) {
        binding.tvSendGuide.text = "${email}으로 인증 코드를 발송했습니다.\n메일을 확인하고 인증코드 6자리를 입력해 주세요."
        binding.tvSendGuide.visibility = View.VISIBLE
        hideEmailError()
    }

    /** 비밀번호 가시성 토글 (눈 아이콘) */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(editText: EditText) {
        editText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val et = v as EditText
                val drawable = et.compoundDrawables[2]
                if (drawable != null && event.rawX >= (et.right - drawable.bounds.width())) {
                    val selection = et.selectionEnd
                    if (et.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        et.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_on, 0)
                    } else {
                        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        et.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0)
                    }
                    et.compoundDrawables[2]?.setTint(ContextCompat.getColor(this, R.color.gray_500))
                    et.setSelection(selection)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}
