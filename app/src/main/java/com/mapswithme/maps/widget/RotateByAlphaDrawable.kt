package com.mapswithme.maps.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import com.mapswithme.util.Graphics

class RotateByAlphaDrawable(
    context: Context?, @DrawableRes resId: Int, @AttrRes tintAttr: Int,
    transparent: Boolean
) : Drawable() {
    private val mBaseDrawable: Drawable
    private var mBounds: Rect? = null
    private var mAngle = 0f
    private var mBaseAngle = 0f
    private fun computeAngle(alpha: Int) {
        mAngle = (alpha - 0xFF) / 3 + mBaseAngle
    }

    fun setBaseAngle(angle: Float): RotateByAlphaDrawable {
        mBaseAngle = angle
        return this
    }

    fun setInnerBounds(bounds: Rect?): RotateByAlphaDrawable {
        mBounds = bounds
        setBounds(mBounds!!)
        return this
    }

    override fun setAlpha(alpha: Int) {
        mBaseDrawable.alpha = alpha
        computeAngle(alpha)
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mBaseDrawable.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return mBaseDrawable.opacity
    }

    override fun onBoundsChange(bounds: Rect) {
        var bounds: Rect? = bounds
        if (mBounds != null) bounds = mBounds
        super.onBoundsChange(bounds)
        mBaseDrawable.bounds = bounds!!
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(
            mAngle,
            mBaseDrawable.bounds.width() / 2.toFloat(),
            mBaseDrawable.bounds.height() / 2.toFloat()
        )
        mBaseDrawable.draw(canvas)
        canvas.restore()
    }

    init {
        mBaseDrawable = Graphics.tint(context!!, resId, tintAttr)!!
        computeAngle(if (transparent) 0x00 else 0xFF)
    }
}