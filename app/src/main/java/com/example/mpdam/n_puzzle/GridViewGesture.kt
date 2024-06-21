package com.example.mpdam.n_puzzle

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.GridView
import kotlin.math.abs
import kotlin.math.roundToInt

class GridViewGesture : GridView {

    private lateinit var gestureDetector: GestureDetector

    private lateinit var flingListener: OnFlingListener

    private var isFlinging: Boolean = false

    private var downX: Float = 0f

    private var downY: Float = 0f

    private var touchSlopThreshold: Int = 0

    constructor(context: Context) : super(context) {
        detectGesture(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        detectGesture(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        detectGesture(context)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes) {
        detectGesture(context)
    }


    fun setFlingListener(flingListener: OnFlingListener) {
        this.flingListener = flingListener
    }

    fun setTouchSlopThreshold(touchSlopThreshold: Int) {
        this.touchSlopThreshold = touchSlopThreshold
    }


    private fun detectGesture(context: Context) {
        gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(event: MotionEvent): Boolean {
                    return true
                }

                override fun onFling(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val position: Int = pointToPosition(e1.x.roundToInt(), e1.y.roundToInt())
                    val direction: FlingDirection = FlingDetector.getDirection(e1, e2)
                    flingListener.onFling(direction, position)

                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev!!.actionMasked == MotionEvent.ACTION_UP) {
            performClick()
        }

        return gestureDetector.onTouchEvent(ev)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            gestureDetector.onTouchEvent(ev)
        }

        when (ev!!.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                isFlinging = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (isFlinging) {
                    return true
                }

                /* Check if the difference between the coordinates is sufficient to be considered a fling. */
                val deltaX: Float = abs(ev.x - downX)
                val deltaY: Float = abs(ev.y - downY)

                if (deltaX > touchSlopThreshold || deltaY > touchSlopThreshold) {
                    isFlinging = true
                    return true
                }
            }
        }

        return super.onInterceptTouchEvent(ev)
    }
}