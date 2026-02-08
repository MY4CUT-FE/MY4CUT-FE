package com.umc.mobile.my4cut

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import com.umc.mobile.my4cut.network.RetrofitClient

class GlobalApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 카카오 SDK 초기화 (필수)
        KakaoSdk.init(this, "91293971b269055877c1f904b3903103")

        // RetrofitClient context 초기화 (필수)
        RetrofitClient.init(this)

        // 해시키 로그 (개발용)
        val keyHash = Utility.getKeyHash(this)
        Log.d("KeyHash", "현재 내 앱의 해시키: $keyHash")
    }
}
