package com.umc.mobile.my4cut.ui.space

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.umc.mobile.my4cut.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SpaceCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val spaces = mutableListOf<Space>()

    fun setSpaces(spaceList: List<Space>) {
        spaces.clear()
        spaces.addAll(spaceList)

        removeAllViews()

        spaces.forEachIndexed { index, space ->
            val itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_space_circle, this, false)

            bindSpace(itemView, space, index)
            addView(itemView)
        }

        requestLayout()
    }

    private fun bindSpace(view: View, space: Space, index: Int) {
        val tvName = view.findViewById<TextView>(R.id.tvSpaceName)
        val tvMember = view.findViewById<TextView>(R.id.tvMemberCount)
        val tvExpire = view.findViewById<TextView>(R.id.tvExpire)

        // 스페이스 이름
        tvName.text = space.name

        // 현재 인원 / 최대 인원
        tvMember.text = "${space.currentMember}/${space.maxMember}"

        // 만료까지 남은 일수 계산
        val remainDays =
            ((space.expiredAt - System.currentTimeMillis()) / (1000 * 60 * 60 * 24))
                .coerceIn(0, 6)

        tvExpire.text = "만료까지 ${remainDays}일"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        val exactSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        setMeasuredDimension(exactSpec, exactSpec)

        if (childCount == 4) {
            val sizesPx = listOf(169, 125, 101, 70)

            for (i in 0 until childCount) {
                val childSize = sizesPx[i]
                val spec = MeasureSpec.makeMeasureSpec(childSize, MeasureSpec.EXACTLY)
                getChildAt(i).measure(spec, spec)
            }
        } else {
            val childSize = (size * 0.38f).toInt()
            val childSpec = MeasureSpec.makeMeasureSpec(childSize, MeasureSpec.EXACTLY)
            for (i in 0 until childCount) {
                getChildAt(i).measure(childSpec, childSpec)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        if (count == 0) return

        if (count == 4) {
            layoutForFourSpaces()
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width / 3f

        for (index in 0 until count) {
            val child = getChildAt(index)
            val angle = 2 * Math.PI * index / count
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            val left = (x - childWidth / 2).toInt()
            val top = (y - childHeight / 2).toInt()
            val right = left + childWidth
            val bottom = top + childHeight

            child.layout(left, top, right, bottom)
        }
    }

    private fun layoutForFourSpaces() {
        val centerX = width / 2f
        val centerY = height / 2f

        // 왼쪽 시안 기준: 크기 비율을 고려한 오프셋
        val offsets = listOf(
            Pair(0.22f, -0.20f),   // 가장 큰 (169)
            Pair(-0.22f, -0.08f),  // 125
            Pair(-0.14f, 0.22f),   // 101
            Pair(0.18f, 0.18f)     // 70
        )

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val (dx, dy) = offsets[index]

            val halfW = child.measuredWidth / 2f
            val halfH = child.measuredHeight / 2f

            val cx = centerX + dx * width
            val cy = centerY + dy * height

            val left = (cx - halfW).toInt()
            val top = (cy - halfH).toInt()
            val right = (cx + halfW).toInt()
            val bottom = (cy + halfH).toInt()

            child.layout(left, top, right, bottom)
        }
    }
}