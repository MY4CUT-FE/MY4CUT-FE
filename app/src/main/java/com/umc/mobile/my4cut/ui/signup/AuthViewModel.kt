package com.umc.mobile.my4cut.ui.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {

    // 이메일 인증 완료 여부
    private val _isEmailVerified = MutableLiveData(false)
    val isEmailVerified: LiveData<Boolean> = _isEmailVerified

    fun setEmailVerified(verified: Boolean) {
        _isEmailVerified.value = verified
    }

    // 비밀번호 형식 검사: 영어/숫자/특수기호만 허용, 8~15자
    fun isValidPassword(password: String): Boolean {
        val regex = Regex("""^[a-zA-Z0-9!@#${'$'}%^&*()_+\-=\[\]{};':"\\|,.<>/?]{8,15}${'$'}""")
        return regex.matches(password)
    }

    // 닉네임 형식 검사: 한글/영어만 허용, 최대 7자
    fun isValidNickname(nickname: String): Boolean {
        val regex = Regex("^[가-힣a-zA-Z]{1,7}$")
        return regex.matches(nickname)
    }
}
