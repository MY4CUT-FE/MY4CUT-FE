package com.umc.mobile.my4cut.ui.pose

import android.content.Context
import android.content.SharedPreferences

object BookmarkManager {
    private const val PREF_NAME = "pose_bookmarks"
    private const val KEY_BOOKMARKS = "bookmarks"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // 즐겨찾기 추가
    fun addBookmark(context: Context, poseId: Int) {
        val prefs = getPrefs(context)
        val bookmarks = getBookmarks(context).toMutableSet()
        bookmarks.add(poseId)
        prefs.edit().putStringSet(KEY_BOOKMARKS, bookmarks.map { it.toString() }.toSet()).apply()
    }

    // 즐겨찾기 제거
    fun removeBookmark(context: Context, poseId: Int) {
        val prefs = getPrefs(context)
        val bookmarks = getBookmarks(context).toMutableSet()
        bookmarks.remove(poseId)
        prefs.edit().putStringSet(KEY_BOOKMARKS, bookmarks.map { it.toString() }.toSet()).apply()
    }

    // 즐겨찾기 여부 확인
    fun isBookmarked(context: Context, poseId: Int): Boolean {
        return getBookmarks(context).contains(poseId)
    }

    // 전체 즐겨찾기 목록 가져오기
    fun getBookmarks(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_BOOKMARKS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }
}