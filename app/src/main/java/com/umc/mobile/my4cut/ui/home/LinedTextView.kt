package com.umc.mobile.my4cut.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet

class LinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#EBEBEB")
        strokeWidth = 1.5f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        val r = Rect()
        val descent = paint.descent().toInt()
        val lineGap = (4 * resources.displayMetrics.density).toInt()
        val offset = descent + lineGap

        // 실제 텍스트 줄
        val count = lineCount
        for (i in 0 until count) {
            val baseline = getLineBounds(i, r)
            canvas.drawLine(
                paddingLeft.toFloat(), (baseline + offset).toFloat(),
                (width - paddingRight).toFloat(), (baseline + offset).toFloat(),
                linePaint
            )
        }

        // 텍스트 이후 빈 줄 채우기
        var lastBaseline = if (count > 0) getLineBounds(count - 1, r) else paddingTop
        val step = lineHeight

        while (true) {
            lastBaseline += step
            if (lastBaseline + offset > height - paddingBottom) break
            canvas.drawLine(
                paddingLeft.toFloat(), (lastBaseline + offset).toFloat(),
                (width - paddingRight).toFloat(), (lastBaseline + offset).toFloat(),
                linePaint
            )
        }

        super.onDraw(canvas)
    }
}