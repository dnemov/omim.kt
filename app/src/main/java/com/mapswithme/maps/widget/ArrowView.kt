package com.mapswithme.maps.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.ImageView

class ArrowView(context: Context?, attrs: AttributeSet?) :
    ImageView(context, attrs) {
    private var mWidth = 0f
    private var mHeight = 0f
    private var mAngle = 0f
    fun setAzimuth(azimuth: Double) {
        mAngle = Math.toDegrees(azimuth).toFloat()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = width.toFloat()
        mHeight = height.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(mAngle, mWidth / 2, mHeight / 2)
        super.onDraw(canvas)
        canvas.restore()
    }
}