package com.umc.mobile.my4cut.ui.login

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    // 인증코드 발송된 이메일 저장
    private var sentEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initClickListeners()
    }

    private fun initClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showEmailError("이메일을 입력해주세요.")
                return@setOnClickListener
            }
            sendVerificationCode(email)
        }

        binding.btnConfirm.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.isEmpty()) {
                showCodeError("인증코드를 입력해주세요.")
                return@setOnClickListener
            }
            verifyCode(sentEmail, code)
        }
    }

    private fun sendVerificationCode(email: String) {
        binding.btnSendCode.isEnabled = false
        hideEmailError()

        RetrofitClient.authServiceNoAuth.sendPasswordResetCode(mapOf("email" to email))
            .enqueue(object : Callback<BaseResponse<Any>> {
                override fun onResponse(
                    call: Call<BaseResponse<Any>>,
                    response: Response<BaseResponse<Any>>
                ) {
                    binding.btnSendCode.isEnabled = true
                    when (response.code()) {
                        200, 201 -> {
                            sentEmail = email
                            showSendGuide(email)
                        }
                        404 -> showEmailError("존재하지 않는 아이디입니다.")
                        else -> {
                            val msg = response.body()?.message ?: "인증코드 발송에 실패했습니다."
                            showEmailError(msg)
                        }
                    }
                }

                override fun onFailure(call: Call<BaseResponse<Any>>, t: Throwable) {
                    binding.btnSendCode.isEnabled = true
                    Toast.makeText(this@ForgotPasswordActivity, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun verifyCode(email: String, code: String) {
        if (email.isEmpty()) {
            showEmailError("먼저 이메일을 입력하고 인증코드를 발송해주세요.")
            return
        }
        hideCodeError()
        // 인증코드 검증은 서버에서 비밀번호 재설정 시 함께 검증하므로
        // 여기서는 형식 확인 후 바로 비밀번호 변경 다이얼로그를 표시
        if (code.length != 6) {
            showCodeError("인증코드 6자리를 입력해주세요.")
            return
        }
        showChangePasswordDialog(email, code)
    }

    private fun showChangePasswordDialog(email: String, code: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)

        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.88).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        setupPasswordToggle(dialogBinding.etNewPassword)
        setupPasswordToggle(dialogBinding.etConfirmPassword)

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnChange.setOnClickListener {
            val newPassword = dialogBinding.etNewPassword.text.toString()
            val confirmPassword = dialogBinding.etConfirmPassword.text.toString()

            dialogBinding.tvNewPasswordError.visibility = View.GONE
            dialogBinding.tvConfirmPasswordError.visibility = View.GONE
            dialogBinding.etNewPassword.setBackgroundResource(R.drawable.bg_edittext_rounded)
            dialogBinding.etConfirmPassword.setBackgroundResource(R.drawable.bg_edittext_rounded)

            if (newPassword.isEmpty()) {
                dialogBinding.tvNewPasswordError.text = "새 비밀번호를 입력해주세요."
                dialogBinding.tvNewPasswordError.visibility = View.VISIBLE
                dialogBinding.etNewPassword.setBackgroundResource(R.drawable.bg_edittext_error)
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                dialogBinding.tvConfirmPasswordError.text = "비밀번호를 재입력해주세요."
                dialogBinding.tvConfirmPasswordError.visibility = View.VISIBLE
                dialogBinding.etConfirmPassword.setBackgroundResource(R.drawable.bg_edittext_error)
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                dialogBinding.tvConfirmPasswordError.text = "비밀번호가 일치하지 않습니다."
                dialogBinding.tvConfirmPasswordError.visibility = View.VISIBLE
                dialogBinding.etConfirmPassword.setBackgroundResource(R.drawable.bg_edittext_error)
                return@setOnClickListener
            }

            resetPassword(email, code, newPassword, dialog, dialogBinding)
        }

        dialog.show()
    }

    private fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        dialog: Dialog,
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
                when (response.code()) {
                    200, 201 -> {
                        dialog.dismiss()
                        Toast.makeText(this@ForgotPasswordActivity, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    400 -> {
                        // 인증코드 불일치
                        dialog.dismiss()
                        showCodeError("인증코드가 일치하지 않습니다.")
                    }
                    409 -> {
                        // 이전과 동일한 비밀번호
                        dialogBinding.tvNewPasswordError.text = "이전과 동일한 비밀번호입니다."
                        dialogBinding.tvNewPasswordError.visibility = View.VISIBLE
                        dialogBinding.etNewPassword.setBackgroundResource(R.drawable.bg_edittext_error)
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

    private fun showEmailError(message: String) {
        binding.tvEmailError.text = message
        binding.tvEmailError.visibility = View.VISIBLE
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_error)
    }

    private fun hideEmailError() {
        binding.tvEmailError.visibility = View.GONE
        binding.etEmail.setBackgroundResource(R.drawable.bg_edittext_rounded)
    }

    private fun showCodeError(message: String) {
        binding.tvCodeError.text = message
        binding.tvCodeError.visibility = View.VISIBLE
        binding.etCode.setBackgroundResource(R.drawable.bg_edittext_error)
    }

    private fun hideCodeError() {
        binding.tvCodeError.visibility = View.GONE
        binding.etCode.setBackgroundResource(R.drawable.bg_edittext_rounded)
    }

    private fun showSendGuide(email: String) {
        binding.tvSendGuide.text = "${email}으로 인증 코드를 발송했습니다.\n메일을 확인하고 인증코드 6자리를 입력해 주세요."
        binding.tvSendGuide.visibility = View.VISIBLE
        hideEmailError()
    }
}
