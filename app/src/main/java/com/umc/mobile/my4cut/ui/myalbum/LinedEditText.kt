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
        val r = Rect()
        val descent = paint.descent().toInt()
        val lineGap = (4 * resources.displayMetrics.density).toInt()
        val offset = descent + lineGap

        val count = lineCount
        for (i in 0 until count) {
            val baseline = getLineBounds(i, r)
            canvas.drawLine(
                r.left.toFloat(), (baseline + offset).toFloat(),
                r.right.toFloat(), (baseline + offset).toFloat(), linePaint
            )
        }

        // 마지막 텍스트 줄 이후 ~ View 높이까지 줄 채우기
        var lastBaseline = if (count > 0) getLineBounds(count - 1, r) else paddingTop
        val step = lineHeight  // lineHeight는 줄 간격 포함한 높이

        while (true) {
            lastBaseline += step
            if (lastBaseline + offset > height - paddingBottom) break
            canvas.drawLine(
                (paddingLeft).toFloat(), (lastBaseline + offset).toFloat(),
                (width - paddingRight).toFloat(), (lastBaseline + offset).toFloat(),
                linePaint
            )
        }

        super.onDraw(canvas)
    }
}