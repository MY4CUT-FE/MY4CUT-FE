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
        color = Color.parseColor("#EBEBEB") // 줄 색상
        strokeWidth = 2f // 줄 두께
    }

    override fun onDraw(canvas: Canvas) {
        // 1. 현재 텍스트의 총 줄 수와 뷰의 전체 높이 확인
        val count = lineCount
        val r = Rect()
        val paint = linePaint

        // 2. 각 텍스트 줄 아래에 선 긋기
        for (i in 0 until count) {
            val baseline = getLineBounds(i, r)
            // 텍스트 바로 아래(baseline + 8dp 정도)에 줄을 긋습니다.
            canvas.drawLine(
                r.left.toFloat(), (baseline + 12).toFloat(),
                r.right.toFloat(), (baseline + 12).toFloat(), paint
            )
        }

        // 3. (선택사항) 만약 텍스트가 없어도 바닥까지 줄을 채우고 싶다면?
        var lastBaseline = getLineBounds(count - 1, r)
        while (lastBaseline < height) {
            lastBaseline += lineHeight
            canvas.drawLine(
                r.left.toFloat(), (lastBaseline + 10).toFloat(),
                r.right.toFloat(), (lastBaseline + 10).toFloat(), paint
            )
        }

        super.onDraw(canvas)
    }
}