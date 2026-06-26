package com.umc.mobile.my4cut.ui.home

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

class SwipeCalendarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var onSwipeListener: ((isRightSwipe: Boolean) -> Unit)? = null

    private var startX = 0f
    private var startY = 0f
    private var isHorizontalSwipe = false

    private val gestureDetector = GestureDetectorCompat(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)
                if (abs(diffX) > abs(diffY) && abs(diffX) > 80 && abs(velocityX) > 80) {
                    onSwipeListener?.invoke(diffX > 0)
                    return true
                }
                return false
            }
        })

    fun setOnSwipeListener(listener: (isRightSwipe: Boolean) -> Unit) {
        onSwipeListener = listener
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                isHorizontalSwipe = false
                gestureDetector.onTouchEvent(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - startX)
                val dy = abs(ev.y - startY)
                if (!isHorizontalSwipe && dx > dy && dx > 20) {
                    isHorizontalSwipe = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (isHorizontalSwipe) {
                    gestureDetector.onTouchEvent(ev)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isHorizontalSwipe = false
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return true
    }
}
