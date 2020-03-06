package com.mapswithme.maps.tips

import android.annotation.TargetApi
import android.graphics.*
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.ColorInt
import com.mapswithme.util.Utils
import uk.co.samuelwall.materialtaptargetprompt.extras.PromptBackground
import uk.co.samuelwall.materialtaptargetprompt.extras.PromptOptions
import uk.co.samuelwall.materialtaptargetprompt.extras.PromptUtils

class ImmersiveModeCompatPromptBackground internal constructor(private val mWindowManager: WindowManager) :
    PromptBackground() {
    private val mBounds: RectF
    private val mBaseBounds: RectF
    private val mPaint: Paint
    private var mBaseColourAlpha = 0
    private val mFocalCentre: PointF
    private val mBaseMetrics: DisplayMetrics
    override fun setColour(@ColorInt colour: Int) {
        mPaint.color = colour
        mBaseColourAlpha = Color.alpha(colour)
        mPaint.alpha = mBaseColourAlpha
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun prepare(
        options: PromptOptions<*>, clipToBounds: Boolean,
        clipBounds: Rect
    ) {
        val focalBounds = options.promptFocal.bounds
        initDisplayMetrics()
        mBaseBounds[0f, 0f, mBaseMetrics.widthPixels.toFloat()] =
            mBaseMetrics.heightPixels.toFloat()
        mFocalCentre.x = focalBounds.centerX()
        mFocalCentre.y = focalBounds.centerY()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun initDisplayMetrics() {
        if (Utils.isJellyBeanOrLater) mWindowManager.defaultDisplay.getRealMetrics(
            mBaseMetrics
        ) else mWindowManager.defaultDisplay.getMetrics(mBaseMetrics)
    }

    override fun update(
        prompt: PromptOptions<*>, revealModifier: Float,
        alphaModifier: Float
    ) {
        mPaint.alpha = (mBaseColourAlpha * alphaModifier).toInt()
        PromptUtils.scale(mFocalCentre, mBaseBounds, mBounds, revealModifier, false)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(mBounds, mPaint)
    }

    override fun contains(x: Float, y: Float): Boolean {
        return mBounds.contains(x, y)
    }

    init {
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mBounds = RectF()
        mBaseBounds = RectF()
        mFocalCentre = PointF()
        mBaseMetrics = DisplayMetrics()
    }
}