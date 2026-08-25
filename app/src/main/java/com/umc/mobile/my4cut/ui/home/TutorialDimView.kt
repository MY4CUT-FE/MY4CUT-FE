package com.umc.mobile.my4cut.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 반투명 딤 배경 위에 지정된 영역만 완전히 투명하게 뚫어(spotlight),
 * 그 영역의 실제 홈 화면 콘텐츠가 딤 없이 원래 밝기 그대로 보이도록 그리는 뷰.
 */
class TutorialDimView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Hole(val rect: RectF, val cornerRadius: Float)

    private val holes = mutableListOf<Hole>()

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99000000.toInt()
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var maskBitmap: Bitmap? = null

    fun setHoles(newHoles: List<Pair<RectF, Float>>) {
        holes.clear()
        newHoles.forEach { (rect, radius) -> holes.add(Hole(rect, radius)) }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maskBitmap?.recycle()
        maskBitmap = if (w > 0 && h > 0) Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) else null
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = maskBitmap ?: return
        val maskCanvas = Canvas(bitmap)
        maskCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
        maskCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        holes.forEach { hole ->
            maskCanvas.drawRoundRect(hole.rect, hole.cornerRadius, hole.cornerRadius, clearPaint)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }
}
