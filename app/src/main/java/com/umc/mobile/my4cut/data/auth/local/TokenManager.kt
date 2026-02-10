package com.umc.mobile.my4cut.data.auth.local

import android.content.Context
import android.util.Log

object TokenManager {

    private const val PREF_NAME = "auth"
    private const val KEY_ACCESS = "accessToken"
    private const val KEY_REFRESH = "refreshToken"

    fun saveTokens(
        context: Context,
        accessToken: String,
        refreshToken: String
    ) {
        Log.d("TokenManager", "Saving tokens...")
        Log.d("TokenManager", "AccessToken: $accessToken")

        val spf = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        spf.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()

        // 토큰 저장 확인
        val saved = spf.getString(KEY_ACCESS, null)
        Log.d("TokenManager", "Saved and retrieved: $saved")
    }

    fun getAccessToken(context: Context): String? {
        val token = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCESS, null)

        Log.d("TokenManager", "Getting AccessToken: $token")
        return token
    }

    fun getRefreshToken(context: Context): String? {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REFRESH, null)
    }

    fun clear(context: Context) {
        Log.d("TokenManager", "Clearing tokens...")
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun getUserId(context: Context): Long? {
        val token = getAccessToken(context) ?: return null

        return try {
            val payload = token.split(".")[1]
            val decodedBytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val json = String(decodedBytes)

            val subValue = org.json.JSONObject(json).getString("sub")
            subValue.toLong()
        } catch (e: Exception) {
            null
        }
    }
}