package com.umc.mobile.my4cut.ui.theme

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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

// 배경(bg_skeleton_img)은 ImageView의 background로 고정해서 카드 전체를 채우고,
// 아이콘(ic_skeleton_img)만 Glide placeholder로 넣어 scaleType=CENTER로 원래 크기 그대로 중앙에 고정한다.
// 사진이 실제로 로드되면 CENTER_CROP으로 전환하고 배경도 지운다. (PhotoRVAdapter와 동일한 패턴)
// onLoaded: 로딩이 끝난 시점(성공/실패 둘 다)에 호출됨 - 같은 아이템의 텍스트 등 다른 스켈레톤 해제용
fun ImageView.loadWithSkeleton(url: String?, onLoaded: () -> Unit = {}) {
    scaleType = ImageView.ScaleType.CENTER
    setBackgroundResource(R.drawable.bg_skeleton_img)

    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_skeleton_img)
        .error(R.drawable.ic_skeleton_img)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                scaleType = ImageView.ScaleType.CENTER
                setBackgroundResource(R.drawable.bg_skeleton_img)
                onLoaded()
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = null
                onLoaded()
                return false
            }
        })
        .into(this)
}