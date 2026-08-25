package com.umc.mobile.my4cut.ui.myalbum

import android.content.Context

object DateSelectTutorialPrefs {

    private const val PREF_NAME = "date_select_tutorial"
    private const val KEY_SEEN = "seenDateSelectTutorial"

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
