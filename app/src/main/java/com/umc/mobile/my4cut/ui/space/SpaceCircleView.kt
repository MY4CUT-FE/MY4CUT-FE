package com.umc.mobile.my4cut.ui.space

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.umc.mobile.my4cut.databinding.ItemSpaceCircleBinding

class SpaceCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding =
        ItemSpaceCircleBinding.inflate(LayoutInflater.from(context), this, true)

    fun applyStyle(style: SpaceCircleStyle) {
        val sizePx = dpToPx(style.circleSizeDp)
        layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)

        binding.tvSpaceName.textSize = style.titleTextSp
        binding.tvMemberCount.textSize = style.memberTextSp
        binding.tvExpire.textSize = style.expireTextSp
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}