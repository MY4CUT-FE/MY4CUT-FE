package com.umc.mobile.my4cut.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

object KeyHashUtil {

    fun printKeyHash(context: Context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )

            for (signature in packageInfo.signatures.orEmpty()) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)

                Log.d("KeyHash", "🔑 Key Hash: $keyHash")
                Log.d("KeyHash", "========================================")
                Log.d("KeyHash", "이 키 해시를 카카오 개발자 콘솔에 등록하세요:")
                Log.d("KeyHash", keyHash)
                Log.d("KeyHash", "========================================")
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "키 해시 확인 실패", e)
        }
    }
}