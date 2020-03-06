package com.mapswithme.maps.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.graphics.drawable.DrawableWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.DrawableCompat
import com.mapswithme.maps.R
import com.mapswithme.util.Graphics
import com.mapswithme.util.ThemeUtils

/**
 * Draws progress wheel, consisting of circle with background and 'stop' button in the center of the circle.
 */
class WheelProgressView : AppCompatImageView {
    private var mProgress = 0
    private var mRadius = 0
    private var mFgPaint: Paint? = null
    private var mBgPaint: Paint? = null
    private var mStrokeWidth = 0
    private val mProgressRect = RectF() // main rect for progress wheel
    private val mCenterRect = RectF() // rect for stop button
    private val mCenter = Point() // center of rect
    private var mIsInit = false
    private var mIsPending = false
    private var mCenterDrawable: Drawable? = null
    private var mPendingDrawable: AnimationDrawable? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?
    ) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.WheelProgressView, 0, 0)
        mStrokeWidth = typedArray.getDimensionPixelSize(
            R.styleable.WheelProgressView_wheelThickness,
            DEFAULT_THICKNESS
        )
        val progressColor = typedArray.getColor(
            R.styleable.WheelProgressView_wheelProgressColor,
            Color.WHITE
        )
        val secondaryColor = typedArray.getColor(
            R.styleable.WheelProgressView_wheelSecondaryColor,
            Color.GRAY
        )
        mCenterDrawable = typedArray.getDrawable(R.styleable.WheelProgressView_centerDrawable)
        if (mCenterDrawable == null) mCenterDrawable =
            makeCenterDrawable(context)
        typedArray.recycle()
        mPendingDrawable = resources.getDrawable(
            ThemeUtils.getResource(
                getContext(),
                R.attr.wheelPendingAnimation
            )
        ) as AnimationDrawable
        Graphics.tint(mPendingDrawable, progressColor)
        mBgPaint = Paint()
        mBgPaint!!.color = secondaryColor
        mBgPaint!!.strokeWidth = mStrokeWidth.toFloat()
        mBgPaint!!.style = Paint.Style.STROKE
        mBgPaint!!.isAntiAlias = true
        mFgPaint = Paint()
        mFgPaint!!.color = progressColor
        mFgPaint!!.strokeWidth = mStrokeWidth.toFloat()
        mFgPaint!!.style = Paint.Style.STROKE
        mFgPaint!!.isAntiAlias = true
    }

    var progress: Int
        get() = mProgress
        set(progress) {
            mProgress = progress
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val left = paddingLeft
        val top = paddingTop
        val right = w - paddingRight
        val bottom = h - paddingBottom
        val width = right - left
        val height = bottom - top
        mRadius = (Math.min(width, height) - mStrokeWidth) / 2
        mCenter[left + width / 2] = top + height / 2
        mProgressRect[mCenter.x - mRadius.toFloat(), mCenter.y - mRadius.toFloat(), mCenter.x + mRadius.toFloat()] =
            mCenter.y + mRadius.toFloat()
        val d =
            if (mCenterDrawable is DrawableWrapper) (mCenterDrawable as DrawableWrapper)
                .wrappedDrawable else mCenterDrawable!!
        if (d is BitmapDrawable) {
            val bmp = d.bitmap
            val halfw = bmp.width / 2
            val halfh = bmp.height / 2
            mCenterDrawable!!.setBounds(
                mCenter.x - halfw,
                mCenter.y - halfh,
                mCenter.x + halfw,
                mCenter.y + halfh
            )
        } else mCenterRect.set(mProgressRect)
        mIsInit = true
    }

    override fun onDraw(canvas: Canvas) {
        if (mIsInit) {
            super.onDraw(canvas)
            if (!mIsPending) {
                canvas.drawCircle(
                    mCenter.x.toFloat(),
                    mCenter.y.toFloat(),
                    mRadius.toFloat(),
                    mBgPaint!!
                )
                canvas.drawArc(
                    mProgressRect,
                    -90f,
                    360 * mProgress / 100.toFloat(),
                    false,
                    mFgPaint!!
                )
            }
            mCenterDrawable!!.draw(canvas)
        }
    }

    var isPending: Boolean
        get() = mIsPending
        set(pending) {
            mIsPending = pending
            if (mIsPending) {
                mPendingDrawable!!.start()
                setImageDrawable(mPendingDrawable)
            } else {
                setImageDrawable(null)
                mPendingDrawable!!.stop()
            }
            invalidate()
        }

    companion object {
        private const val DEFAULT_THICKNESS = 4
        private fun makeCenterDrawable(context: Context): Drawable {
            val normalDrawable =
                context.resources.getDrawable(R.drawable.ic_close_spinner)
            val wrapped = DrawableCompat.wrap(normalDrawable)
            DrawableCompat.setTint(
                wrapped.mutate(),
                ThemeUtils.getColor(context, R.attr.iconTint)
            )
            return normalDrawable
        }
    }
}