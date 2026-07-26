package com.umc.mobile.my4cut.ui.theme

import android.view.View
import android.widget.ImageView
import coil.load
import com.umc.mobile.my4cut.R

fun View.showSkeleton() {
    visibility = View.VISIBLE
}

fun View.hideSkeleton() {
    visibility = View.GONE
}

fun View.showContent() {
    visibility = View.VISIBLE
}

fun View.hideContent() {
    visibility = View.GONE
}

// 이미지 로드 전엔 ic_skeleton_img + bg_skeleton_img를 보여주고, 로드 완료 시 원래 모습으로 복원
fun ImageView.loadWithSkeleton(
    url: String?,
    scaleTypeOnLoaded: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
) {
    setImageResource(R.drawable.ic_skeleton_img)
    setBackgroundResource(R.drawable.bg_skeleton_img)
    scaleType = ImageView.ScaleType.CENTER

    load(url) {
        crossfade(true)
        listener(
            onSuccess = { _, _ ->
                background = null
                scaleType = scaleTypeOnLoaded
            },
            onError = { _, _ ->
                setImageResource(R.drawable.ic_skeleton_img)
                setBackgroundResource(R.drawable.bg_skeleton_img)
                scaleType = ImageView.ScaleType.CENTER
            }
        )
    }
}