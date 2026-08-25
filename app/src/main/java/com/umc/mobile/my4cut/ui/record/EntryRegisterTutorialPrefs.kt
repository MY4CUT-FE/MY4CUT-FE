package com.umc.mobile.my4cut.ui.record

import android.content.Context

object EntryRegisterTutorialPrefs {

    private const val PREF_NAME = "entry_register_tutorial"
    private const val KEY_SEEN = "seenEntryRegisterTutorial"

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
