package com.umc.mobile.my4cut.ui.pose

import android.content.Context
import android.content.SharedPreferences
import com.umc.mobile.my4cut.data.auth.local.TokenManager

object BookmarkManager {
    private const val PREF_NAME = "pose_bookmarks"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * ✅ 사용자별 즐겨찾기 키 생성
     * 로그인한 사용자 ID를 기반으로 고유한 키 생성
     */
    private fun getUserBookmarkKey(context: Context): String {
        val userId = TokenManager.getUserId(context) ?: 0L
        return "bookmarks_user_$userId"
    }

    // 즐겨찾기 추가
    fun addBookmark(context: Context, poseId: Int) {
        val prefs = getPrefs(context)
        val key = getUserBookmarkKey(context)
        val bookmarks = getBookmarks(context).toMutableSet()
        bookmarks.add(poseId)
        prefs.edit().putStringSet(key, bookmarks.map { it.toString() }.toSet()).apply()
    }

    // 즐겨찾기 제거
    fun removeBookmark(context: Context, poseId: Int) {
        val prefs = getPrefs(context)
        val key = getUserBookmarkKey(context)
        val bookmarks = getBookmarks(context).toMutableSet()
        bookmarks.remove(poseId)
        prefs.edit().putStringSet(key, bookmarks.map { it.toString() }.toSet()).apply()
    }

    // 즐겨찾기 여부 확인
    fun isBookmarked(context: Context, poseId: Int): Boolean {
        return getBookmarks(context).contains(poseId)
    }

    // 전체 즐겨찾기 목록 가져오기
    fun getBookmarks(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val key = getUserBookmarkKey(context)
        return prefs.getStringSet(key, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }

    /**
     * ✅ 로그아웃 시 현재 사용자의 즐겨찾기 데이터 삭제 (선택사항)
     */
    fun clearUserBookmarks(context: Context) {
        val prefs = getPrefs(context)
        val key = getUserBookmarkKey(context)
        prefs.edit().remove(key).apply()
    }
}