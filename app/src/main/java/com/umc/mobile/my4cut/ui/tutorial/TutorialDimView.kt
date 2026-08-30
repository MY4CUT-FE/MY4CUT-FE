package com.umc.mobile.my4cut.ui.tutorial

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class TutorialDimView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#99000000")
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val highlightRects =
        mutableListOf<Pair<RectF, Float>>()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun clearHighlights() {
        highlightRects.clear()
        invalidate()
    }

    fun addHighlight(
        rect: RectF,
        radius: Float = 0f
    ) {
        highlightRects.add(rect to radius)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            dimPaint
        )

        highlightRects.forEach { (rect, radius) ->
            canvas.drawRoundRect(
                rect,
                radius,
                radius,
                clearPaint
            )
        }
    }
}