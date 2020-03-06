package com.mapswithme.maps.widget

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.mapswithme.maps.MwmApplication

class RotateDrawable(private val mBaseDrawable: Drawable?) :
    Drawable() {
    private var mAngle = 0f

    constructor(@DrawableRes resId: Int) : this(
        ContextCompat.getDrawable(
            MwmApplication.get(),
            resId
        )
    ) {
    }

    override fun setAlpha(alpha: Int) {
        mBaseDrawable!!.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mBaseDrawable!!.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return mBaseDrawable!!.opacity
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        mBaseDrawable!!.setBounds(
            0, 0, mBaseDrawable.intrinsicWidth,
            mBaseDrawable.intrinsicHeight
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(mAngle, bounds.width() * 0.5f, bounds.height() * 0.5f)
        canvas.translate(
            (bounds.width() - mBaseDrawable!!.intrinsicWidth) * 0.5f,
            (bounds.height() - mBaseDrawable.intrinsicHeight) * 0.5f
        )
        mBaseDrawable.draw(canvas)
        canvas.restore()
    }

    fun setAngle(angle: Float) {
        mAngle = angle
        invalidateSelf()
    }

}