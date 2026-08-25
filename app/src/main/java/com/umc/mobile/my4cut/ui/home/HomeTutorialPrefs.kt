package com.umc.mobile.my4cut.ui.home

import android.content.Context

object HomeTutorialPrefs {

    private const val PREF_NAME = "home_tutorial"
    private const val KEY_SEEN = "seenHomeTutorial"

    fun hasSeenTutorial(context: Context): Boolean {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEN, false)
    }

    fun setTutorialSeen(context: Context) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEN, true)
            .apply()
    }
}
