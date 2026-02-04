package com.umc.mobile.my4cut.data.auth.local

import android.content.Context

object TokenManager {

    private const val PREF_NAME = "auth"
    private const val KEY_ACCESS = "accessToken"
    private const val KEY_REFRESH = "refreshToken"

    fun saveTokens(
        context: Context,
        accessToken: String,
        refreshToken: String
    ) {
        val spf = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        spf.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    fun getAccessToken(context: Context): String? {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCESS, null)
    }

    fun getRefreshToken(context: Context): String? {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REFRESH, null)
    }

    fun clear(context: Context) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
