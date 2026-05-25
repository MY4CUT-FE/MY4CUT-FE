package com.umc.mobile.my4cut.ui.myalbum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet

class LinedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#EBEBEB") // 줄 색상 (홈 화면 renderDiaryLines와 동일)
        strokeWidth = 1.5f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        val count = lineCount
        val r = Rect()
        val descent = paint.descent().toInt()
        val lineGap = (4 * resources.displayMetrics.density).toInt()
        val offset = descent + lineGap

        for (i in 0 until count) {
            val baseline = getLineBounds(i, r)
            canvas.drawLine(
                r.left.toFloat(), (baseline + offset).toFloat(),
                r.right.toFloat(), (baseline + offset).toFloat(), linePaint
            )
        }

        var lastBaseline = getLineBounds(count - 1, r)
        while (lastBaseline < height) {
            lastBaseline += lineHeight
            canvas.drawLine(
                r.left.toFloat(), (lastBaseline + offset).toFloat(),
                r.right.toFloat(), (lastBaseline + offset).toFloat(), linePaint
            )
        }

        super.onDraw(canvas)
    }
}