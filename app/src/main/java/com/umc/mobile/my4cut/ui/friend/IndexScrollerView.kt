package com.umc.mobile.my4cut.ui.friend

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.floor

class IndexScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val letters = listOf(
        "★","ㄱ","ㄴ","ㄷ","ㄹ","ㅁ","ㅂ","ㅅ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        textSize = 22f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    var onLetterTouched: ((String) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val itemHeight = height / letters.size.toFloat()
        val centerX = width / 2f

        letters.forEachIndexed { index, letter ->
            val y = itemHeight * index + itemHeight / 2f + paint.textSize / 3
            canvas.drawText(letter, centerX, y, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val index = floor(event.y / height * letters.size).toInt()
            .coerceIn(0, letters.lastIndex)

        onLetterTouched?.invoke(letters[index])
        return true
    }
}