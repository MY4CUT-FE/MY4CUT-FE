package com.umc.mobile.my4cut.util

import android.app.Activity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.umc.mobile.my4cut.R

/**
 * targetSdk 35+ (Android 15+) 에서 강제되는 edge-to-edge 대응.
 */
fun Activity.applySystemBarInsets() {
    val content = findViewById<View>(android.R.id.content)
    content.setBackgroundColor(ContextCompat.getColor(this, R.color.white))

    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = true
        isAppearanceLightNavigationBars = true
    }

    ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        // 키보드가 올라오면 기존 adjustResize 동작처럼 키보드 높이만큼 콘텐츠를 올림
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
        // 하위 뷰(BottomNavigationView 등)가 inset을 중복 적용하지 않도록 여기서 소비
        WindowInsetsCompat.CONSUMED
    }
}
