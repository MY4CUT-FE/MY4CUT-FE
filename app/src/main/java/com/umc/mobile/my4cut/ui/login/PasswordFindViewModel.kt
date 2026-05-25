package com.umc.mobile.my4cut.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PasswordFindViewModel : ViewModel() {

    // 이메일 인증 완료 여부
    private val _isEmailVerified = MutableLiveData(false)
    val isEmailVerified: LiveData<Boolean> = _isEmailVerified

    // 인증 완료된 이메일 저장
    private val _verifiedEmail = MutableLiveData("")
    val verifiedEmail: LiveData<String> = _verifiedEmail

    fun setEmailVerified(verified: Boolean, email: String = "") {
        _isEmailVerified.value = verified
        if (verified) _verifiedEmail.value = email
    }

    // 비밀번호 형식 검사: 영어/숫자/특수기호만 허용, 8~15자 (AuthViewModel과 동일)
    fun isValidPassword(password: String): Boolean {
        val regex = Regex("""^[a-zA-Z0-9!@#${'$'}%^&*()_+\-=\[\]{};':"\\|,.<>/?]{8,15}${'$'}""")
        return regex.matches(password)
    }
}
