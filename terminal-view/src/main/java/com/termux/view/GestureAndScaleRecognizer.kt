package com.termux.view

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 * A combination of {@link GestureDetector} and {@link ScaleGestureDetector}.
 *
 * Note: package-private in Java and widened to public because Kotlin has no
 * package-private; the still-Java TerminalView instantiates it.
 */
class GestureAndScaleRecognizer(context: Context?, listener: Listener) {

    interface Listener {

        fun onSingleTapUp(e: MotionEvent?): Boolean

        fun onDoubleTap(e: MotionEvent?): Boolean

        fun onScroll(e2: MotionEvent?, dx: Float, dy: Float): Boolean

        fun onFling(e: MotionEvent?, velocityX: Float, velocityY: Float): Boolean

        fun onScale(focusX: Float, focusY: Float, scale: Float): Boolean

        fun onDown(x: Float, y: Float): Boolean

        fun onUp(e: MotionEvent?): Boolean

        fun onLongPress(e: MotionEvent?)
    }

    private val mGestureDetector: GestureDetector

    private val mScaleDetector: ScaleGestureDetector

    @JvmField
    val mListener: Listener = listener

    @JvmField
    var isAfterLongPress = false

    init {
        mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                return mListener.onScroll(e2, distanceX, distanceY)
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                return mListener.onFling(e2, velocityX, velocityY)
            }

            override fun onDown(e: MotionEvent): Boolean {
                // Null event threw NPE here in Java as well.
                return mListener.onDown(e.x, e.y)
            }

            override fun onLongPress(e: MotionEvent) {
                mListener.onLongPress(e)
                isAfterLongPress = true
            }
        }, null, true)
        mGestureDetector.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return mListener.onSingleTapUp(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                return mListener.onDoubleTap(e)
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return true
            }
        })
        mScaleDetector = ScaleGestureDetector(
            // Null context threw NPE here in Java as well; the stub is strict.
            context!!,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                return mListener.onScale(detector.focusX, detector.focusY, detector.scaleFactor)
            }
        })
        mScaleDetector.setQuickScaleEnabled(false)
    }

    fun onTouchEvent(event: MotionEvent?) {
        // Null event threw NPE below in Java as well.
        val nonNullEvent = event!!
        mGestureDetector.onTouchEvent(nonNullEvent)
        mScaleDetector.onTouchEvent(nonNullEvent)
        when (nonNullEvent.action) {
            MotionEvent.ACTION_DOWN ->
                isAfterLongPress = false
            MotionEvent.ACTION_UP ->
                if (!isAfterLongPress) {
                    // This behaviour is desired when in e.g. vim with mouse events, where we do not
                    // want to move the cursor when lifting finger after a long press.
                    mListener.onUp(nonNullEvent)
                }
        }
    }

    fun isInProgress(): Boolean {
        return mScaleDetector.isInProgress
    }
}
