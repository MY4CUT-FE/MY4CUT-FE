package com.umc.mobile.my4cut.ui.space

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.umc.mobile.my4cut.R

class ExpireCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    var progress: Float = 0f   // 생성 직후 비어 있음 (0f ~ 1f)
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.gray_300)
        strokeWidth = dpToPx(2f)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.coral_700)
        strokeWidth = dpToPx(3f)
    }

    private val rect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val maxStroke = maxOf(trackPaint.strokeWidth, progressPaint.strokeWidth)
        val radiusPadding = maxStroke / 2f

        rect.set(
            radiusPadding,
            radiusPadding,
            width - radiusPadding,
            height - radiusPadding
        )

        // 회색 트랙 (전체 원)
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        // 진행도 (12시 방향 시작)
        canvas.drawArc(
            rect,
            -90f,
            360f * progress,
            false,
            progressPaint
        )
    }

    private fun dpToPx(dp: Float): Float =
        dp * resources.displayMetrics.density

    fun setExpireInfo(createdAt: Long, expiredAt: Long) {
        val totalDuration = expiredAt - createdAt

        if (totalDuration <= 0L) {
            progress = 1f
            return
        }

        val elapsed = (System.currentTimeMillis() - createdAt)
            .coerceAtLeast(0L)

        progress = (elapsed.toFloat() / totalDuration.toFloat())
            .coerceIn(0f, 1f)
    }
}