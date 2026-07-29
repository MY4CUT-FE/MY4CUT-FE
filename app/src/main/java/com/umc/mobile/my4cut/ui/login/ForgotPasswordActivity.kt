package com.umc.mobile.my4cut.ui.login

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.JsonParser
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.auth.model.EmailVerifyResult
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
    // 검증 완료 시 발급되는 토큰 (비밀번호 재설정 API 호출 시 code 대신 전달)
    private var verificationToken: String = ""

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

    /** POST /auth/email/password-reset/send - 비밀번호 재설정 인증코드 발송 */
    private fun sendVerificationCode(email: String) {
        binding.btnSendCode.isEnabled = false
        hideEmailError()

        RetrofitClient.authServiceNoAuth.sendPasswordResetEmailCode(mapOf("email" to email))
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    binding.btnSendCode.isEnabled = true
                    val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    Log.d("ForgotPw_Send", "http=${response.code()}, body=${response.body()}, errorBody=$errorBody")

                    if (response.isSuccessful) {
                        sentEmail = email
                        viewModel.setEmailVerified(false)
                        verificationToken = ""
                        showSendGuide(email)
                    } else {
                        val msg = extractErrorMessage(errorBody)
                            ?: "인증코드 발송에 실패했습니다. (${response.code()})"
                        showEmailError(msg)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    binding.btnSendCode.isEnabled = true
                    Log.e("ForgotPw_Send", "failure", t)
                    Toast.makeText(this@ForgotPasswordActivity, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** POST /auth/email/password-reset/verify - 인증코드 검증 */
    private fun verifyCode(email: String, code: String) {
        if (email.isEmpty()) {
            showEmailError("먼저 이메일을 입력하고 인증코드를 발송해주세요.")
            return
        }
        hideCodeError()
        binding.btnConfirm.isEnabled = false

        RetrofitClient.authServiceNoAuth.verifyPasswordResetEmailCode(mapOf("email" to email, "code" to code))
            .enqueue(object : Callback<BaseResponse<EmailVerifyResult>> {
                override fun onResponse(
                    call: Call<BaseResponse<EmailVerifyResult>>,
                    response: Response<BaseResponse<EmailVerifyResult>>
                ) {
                    binding.btnConfirm.isEnabled = true
                    Log.d("ForgotPw_Verify", "http=${response.code()}, body=${response.body()}")

                    if (response.isSuccessful) {
                        // 인증 성공 → 응답으로 받은 verificationToken을 저장 후 비밀번호 변경 모달 표시
                        verificationToken = response.body()?.data?.verificationToken ?: ""
                        viewModel.setEmailVerified(true, email)
                        binding.etCode.setBackgroundResource(R.drawable.bg_edittext_success)
                        showCodeSuccess("인증이 완료되었습니다.")
                        showChangePasswordDialog(email, verificationToken)
                    } else {
                        val message = when (response.code()) {
                            400 -> "인증코드가 일치하지 않습니다."
                            410 -> "인증코드가 만료되었습니다. 다시 발송해주세요."
                            else -> "인증에 실패했습니다. (${response.code()})"
                        }
                        showCodeError(message)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<EmailVerifyResult>>, t: Throwable) {
                    binding.btnConfirm.isEnabled = true
                    Log.e("ForgotPw_Verify", "failure", t)
                    Toast.makeText(this@ForgotPasswordActivity, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** 비밀번호 변경 모달을 화면 중앙에 표시 */
    private fun showChangePasswordDialog(email: String, verificationToken: String) {
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        setupPasswordToggle(dialogBinding.etNewPassword)
        setupPasswordToggle(dialogBinding.etConfirmPassword)

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

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

            // 비밀번호 형식 검사: 영어/숫자/특수기호를 모두 포함, 8~15자
            if (!viewModel.isValidPassword(newPassword)) {
                showDialogError(dialogBinding.tvNewPasswordError, dialogBinding.etNewPassword, "영어/숫자/특수기호를 모두 사용해 8~15자로 설정해주세요.")
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

            resetPassword(email, verificationToken, newPassword, dialog, dialogBinding)
        }

        dialog.show()
    }

    /** POST /auth/password/reset - 비밀번호 재설정 */
    private fun resetPassword(
        email: String,
        verificationToken: String,
        newPassword: String,
        dialog: Dialog,
        dialogBinding: DialogChangePasswordBinding
    ) {
        dialogBinding.btnChange.isEnabled = false

        RetrofitClient.authServiceNoAuth.resetPassword(
            PasswordResetRequest(email = email, verificationToken = verificationToken, newPassword = newPassword)
        ).enqueue(object : Callback<BaseResponse<Any>> {
            override fun onResponse(
                call: Call<BaseResponse<Any>>,
                response: Response<BaseResponse<Any>>
            ) {
                dialogBinding.btnChange.isEnabled = true
                val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                Log.d("ForgotPw_Reset", "http=${response.code()}, body=${response.body()}, errorBody=$errorBody")

                when (response.code()) {
                    200, 201 -> {
                        dialog.dismiss()
                        Toast.makeText(this@ForgotPasswordActivity, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    400 -> {
                        // 모달 닫고 메인 화면에 코드 에러 표시 (서버가 준 실제 메시지 우선 사용)
                        dialog.dismiss()
                        showCodeError(extractErrorMessage(errorBody) ?: "인증코드가 일치하지 않습니다.")
                    }
                    409 -> {
                        // 이전과 동일한 비밀번호 (새 비밀번호 + 재입력 칸 모두 표시)
                        val message = "이전과 동일한 비밀번호입니다."
                        showDialogError(dialogBinding.tvNewPasswordError, dialogBinding.etNewPassword, message)
                        showDialogError(dialogBinding.tvConfirmPasswordError, dialogBinding.etConfirmPassword, message)
                    }
                    else -> {
                        val msg = extractErrorMessage(errorBody) ?: "비밀번호 변경에 실패했습니다."
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

    /** 에러 응답 body(JSON)에서 message 필드 추출 */
    private fun extractErrorMessage(errorBody: String?): String? {
        return try {
            errorBody?.let { JsonParser.parseString(it).asJsonObject.get("message")?.asString }
        } catch (e: Exception) {
            null
        }
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
        binding.tvCodeError.setTextColor(color)
        binding.tvCodeError.compoundDrawables[0]?.setTint(color)
        binding.etCode.setBackgroundResource(R.drawable.bg_edittext_error)
    }

    /** 인증코드 검증 완료 표시 */
    private fun showCodeSuccess(message: String) {
        binding.tvCodeError.text = message
        binding.tvCodeError.visibility = View.VISIBLE
        binding.tvCodeError.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
        val color = ContextCompat.getColor(this, R.color.success_green)
        binding.tvCodeError.setTextColor(color)
        binding.tvCodeError.compoundDrawables[0]?.setTint(color)
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
