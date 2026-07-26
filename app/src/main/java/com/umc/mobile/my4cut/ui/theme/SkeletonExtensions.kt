package com.umc.mobile.my4cut.ui.theme

import android.os.SystemClock
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
// 캐시에 이미 있는 이미지는 onSuccess가 거의 즉시 호출되어 스켈레톤이 한 프레임도 안 보이고 지나가버리는데,
// 이를 방지하기 위해 최소 MIN_SKELETON_DURATION_MS만큼은 스켈레톤이 보이도록 지연 처리함
fun ImageView.loadWithSkeleton(
    url: String?,
    scaleTypeOnLoaded: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
) {
    tag = url // 재활용된 뷰에 지연 콜백이 잘못 적용되는 것을 막기 위한 식별자

    setImageResource(R.drawable.ic_skeleton_img)
    setBackgroundResource(R.drawable.bg_skeleton_img)
    scaleType = ImageView.ScaleType.CENTER

    val startTime = SystemClock.elapsedRealtime()

    load(url) {
        crossfade(true)
        listener(
            onSuccess = { _, _ ->
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val remaining = MIN_SKELETON_DURATION_MS - elapsed

                val applyResult: () -> Unit = {
                    if (tag == url) { // 그 사이 다른 URL로 재사용되지 않았을 때만 적용
                        background = null
                        scaleType = scaleTypeOnLoaded
                    }
                }

                if (remaining > 0) {
                    postDelayed(applyResult, remaining)
                } else {
                    applyResult()
                }
            },
            onError = { _, _ ->
                if (tag == url) {
                    setImageResource(R.drawable.ic_skeleton_img)
                    setBackgroundResource(R.drawable.bg_skeleton_img)
                    scaleType = ImageView.ScaleType.CENTER
                }
            }
        )
    }
}

private const val MIN_SKELETON_DURATION_MS = 250L