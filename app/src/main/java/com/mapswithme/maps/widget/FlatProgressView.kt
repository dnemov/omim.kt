package com.mapswithme.maps.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.mapswithme.maps.R

class FlatProgressView : View {
    private var mThickness = 0
    private var mSecondaryThickness = 0
    private var mHeadRadius = 0
    private var mProgress = 0
    private val mProgressPaint = Paint()
    private val mSecondaryProgressPaint = Paint()
    private val mHeadPaint = Paint()
    private val mHeadRect = RectF()
    private var mReady = false

    constructor(context: Context?) : super(context) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init(attrs, 0)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        if (isInEditMode) return
        val ta = context.obtainStyledAttributes(
            attrs,
            R.styleable.FlatProgressView,
            defStyleAttr,
            0
        )
        thickness = ta.getDimensionPixelSize(R.styleable.FlatProgressView_progressThickness, 1)
        secondaryThickness = ta.getDimensionPixelSize(
            R.styleable.FlatProgressView_secondaryProgressThickness,
            1
        )
        headRadius = ta.getDimensionPixelSize(R.styleable.FlatProgressView_headRadius, 4)
        progress = ta.getInteger(R.styleable.FlatProgressView_progress, 0)
        var color =
            ta.getColor(R.styleable.FlatProgressView_progressColor, Color.BLUE)
        mProgressPaint.color = color
        color = ta.getColor(
            R.styleable.FlatProgressView_secondaryProgressColor,
            Color.GRAY
        )
        mSecondaryProgressPaint.color = color
        color = ta.getColor(R.styleable.FlatProgressView_headColor, Color.BLUE)
        mHeadPaint.color = color
        ta.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            measuredWidth,
            Math.max(
                mSecondaryThickness,
                Math.max(mThickness, mHeadRadius * 2)
            ) + paddingTop + paddingBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        mReady = true
        canvas.save()
        val intWidth = width - paddingLeft - paddingRight
        val intHeight = height - paddingTop - paddingBottom
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        val progressWidth = intWidth * mProgress / 100
        if (progressWidth > 0) {
            val top =
                if (mHeadRadius * 2 > mThickness) mHeadRadius - mThickness / 2 else 0
            canvas.drawRect(
                0f,
                top.toFloat(),
                progressWidth.toFloat(),
                top + mThickness.toFloat(),
                mProgressPaint
            )
        }
        if (mProgress < 100) {
            val top = (intHeight - mSecondaryThickness) / 2
            canvas.drawRect(
                progressWidth.toFloat(),
                top.toFloat(),
                intWidth.toFloat(),
                top + mSecondaryThickness.toFloat(),
                mSecondaryProgressPaint
            )
        }
        if (mHeadRadius > 0) {
            val top =
                if (mHeadRadius * 2 > mThickness) 0 else mThickness / 2 - mHeadRadius
            canvas.translate(progressWidth - mHeadRadius.toFloat(), top.toFloat())
            canvas.drawOval(mHeadRect, mHeadPaint)
        }
        canvas.restore()
    }

    var progress: Int
        get() = mProgress
        set(progress) {
            if (mProgress == progress) return
            require(!(progress < 0 || progress > 100)) { "Progress must be within interval [0..100]" }
            mProgress = progress
            if (mReady) invalidate()
        }

    var thickness: Int
        get() = mThickness
        set(thickness) {
            if (thickness == mThickness) return
            mThickness = thickness
            if (mReady) invalidate()
        }

    var headRadius: Int
        get() = mHeadRadius
        set(headRadius) {
            if (headRadius == mHeadRadius) return
            mHeadRadius = headRadius
            mHeadRect[0.0f, 0.0f, mHeadRadius * 2.toFloat()] = mHeadRadius * 2.toFloat()
            if (mReady) invalidate()
        }

    fun setProgressColor(color: Int) {
        if (color == mProgressPaint.color) return
        mProgressPaint.color = color
        if (mReady) invalidate()
    }

    fun setHeadColor(color: Int) {
        if (color == mHeadPaint.color) return
        mHeadPaint.color = color
        if (mReady) invalidate()
    }

    fun setSecondaryProgressColor(color: Int) {
        if (color == mSecondaryProgressPaint.color) return
        mSecondaryProgressPaint.color = color
        if (mReady) invalidate()
    }

    var secondaryThickness: Int
        get() = mSecondaryThickness
        set(secondaryThickness) {
            if (secondaryThickness == mSecondaryThickness) return
            mSecondaryThickness = secondaryThickness
            if (mReady) invalidate()
        }
}