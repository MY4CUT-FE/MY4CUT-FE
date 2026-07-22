package com.umc.mobile.my4cut.ui.theme

import android.view.View

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