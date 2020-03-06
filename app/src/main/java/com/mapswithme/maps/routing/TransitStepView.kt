package com.mapswithme.maps.routing

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.DrawableCompat
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.recycler.MultilineLayoutManager.SqueezingInterface
import com.mapswithme.util.ThemeUtils

/**
 * Represents a specific transit step. It displays a transit info, such as a number, color, etc., for
 * the specific transit type: pedestrian, rail, metro, etc.
 */
class TransitStepView : View, SqueezingInterface {
    private var mDrawable: Drawable? = null
    private val mBackgroundBounds = RectF()
    private val mDrawableBounds = Rect()
    private val mTextBounds = Rect()
    private val mBackgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var mBackgroundCornerRadius = 0
    private var mText: String? = null
    private lateinit var mStepType: TransitStepType

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init(attrs)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        mBackgroundCornerRadius =
            resources.getDimensionPixelSize(R.dimen.routing_transit_step_corner_radius)
        val a = context.obtainStyledAttributes(attrs, R.styleable.TransitStepView)
        val textSize =
            a.getDimensionPixelSize(R.styleable.TransitStepView_android_textSize, 0).toFloat()
        @ColorInt val textColor =
            a.getColor(R.styleable.TransitStepView_android_textColor, Color.BLACK)
        mTextPaint.textSize = textSize
        mTextPaint.color = textColor
        mDrawable = a.getDrawable(R.styleable.TransitStepView_android_drawable)
        if (mDrawable != null) mDrawable = DrawableCompat.wrap(mDrawable!!)
        mStepType = TransitStepType.PEDESTRIAN
        a.recycle()
    }

    fun setTransitStepInfo(info: TransitStepInfo) {
        mStepType = info.type
        mDrawable = resources.getDrawable(
            if (mStepType == TransitStepType.INTERMEDIATE_POINT) getIntermediatePointDrawableId(
                info.intermediateIndex
            ) else mStepType.drawable
        )
        mDrawable = DrawableCompat.wrap(mDrawable!!)
        mBackgroundPaint.color = getBackgroundColor(context, info)
        mText = info.number
        invalidate()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var height =
            getDefaultSize(suggestedMinimumHeight, MeasureSpec.UNSPECIFIED)
        var width = paddingLeft
        if (mDrawable != null) {
            calculateDrawableBounds(height, mDrawable!!)
            width += mDrawableBounds.width()
        }
        if (!TextUtils.isEmpty(mText)) {
            mTextPaint.getTextBounds(mText, 0, mText!!.length, mTextBounds)
            width += (if (mDrawable != null) paddingLeft else 0) + mTextPaint.measureText(mText).toInt()
            if (height == 0) height = paddingTop + mTextBounds.height() + paddingBottom
        }
        width += paddingRight
        mBackgroundBounds[0f, 0f, width.toFloat()] = height.toFloat()
        setMeasuredDimension(width, height)
    }

    override fun squeezeTo(@Dimension width: Int) {
        val tSize =
            width - 2 * paddingLeft - paddingRight - mDrawableBounds.width()
        mText = TextUtils.ellipsize(mText, mTextPaint, tSize.toFloat(), TextUtils.TruncateAt.END)
            .toString()
    }

    override val minimumAcceptableSize: Int
        get() = resources.getDimensionPixelSize(R.dimen.routing_transit_setp_min_acceptable_with)

    private fun calculateDrawableBounds(
        height: Int,
        drawable: Drawable
    ) { // If the clear view height, i.e. without top/bottom padding, is greater than the drawable height
// the drawable should be centered vertically by adding additional vertical top/bottom padding.
// If the drawable height is greater than the clear view height the drawable will be fitted
// (squeezed) into the parent container.
        val clearHeight = height - paddingTop - paddingBottom
        var vPad = 0
        if (clearHeight >= drawable.intrinsicHeight) vPad =
            (clearHeight - drawable.intrinsicHeight) / 2
        val acceptableDrawableHeight =
            if (clearHeight >= drawable.intrinsicHeight) drawable.intrinsicHeight else clearHeight
        // A transit icon must be squared-shaped. So, if the drawable width is greater than height the
// drawable will be squeezed horizontally to make it squared-shape.
        val acceptableDrawableWidth =
            if (drawable.intrinsicWidth > acceptableDrawableHeight) acceptableDrawableHeight else drawable.intrinsicWidth
        mDrawableBounds[paddingLeft, paddingTop + vPad, acceptableDrawableWidth + paddingLeft] =
            paddingTop + vPad + acceptableDrawableHeight
    }

    override fun onDraw(canvas: Canvas) {
        if (background == null && mDrawable != null) {
            canvas.drawRoundRect(
                mBackgroundBounds,
                mBackgroundCornerRadius.toFloat(),
                mBackgroundCornerRadius.toFloat(),
                mBackgroundPaint
            )
        }
        if (mDrawable != null) drawDrawable(context, mStepType, mDrawable!!, canvas)
        if (!TextUtils.isEmpty(mText)) {
            val yPos =
                (canvas.height / 2 - (mTextPaint.descent() + mTextPaint.ascent()) / 2).toInt()
            val xPos =
                if (mDrawable != null) mDrawable!!.bounds.right + paddingLeft else paddingLeft
            canvas.drawText(mText!!, xPos.toFloat(), yPos.toFloat(), mTextPaint)
        }
    }

    private fun drawDrawable(
        context: Context, type: TransitStepType,
        drawable: Drawable, canvas: Canvas
    ) {
        if (type == TransitStepType.PEDESTRIAN) {
            drawable.mutate()
            DrawableCompat.setTint(
                drawable,
                ThemeUtils.getColor(context, R.attr.iconTint)
            )
        } else if (type == TransitStepType.INTERMEDIATE_POINT) {
            drawable.mutate()
            DrawableCompat.setTint(
                drawable,
                resources.getColor(R.color.routing_intermediate_point)
            )
        }
        drawable.bounds = mDrawableBounds
        drawable.draw(canvas)
    }

    companion object {
        @ColorInt
        private fun getBackgroundColor(
            context: Context,
            info: TransitStepInfo
        ): Int {
            return when (info.type) {
                TransitStepType.PEDESTRIAN -> ThemeUtils.getColor(
                    context,
                    R.attr.transitPedestrianBackground
                )
                TransitStepType.INTERMEDIATE_POINT -> Color.TRANSPARENT
                else -> info.color
            }
        }

        @DrawableRes
        private fun getIntermediatePointDrawableId(index: Int): Int {
            when (index) {
                0 -> return R.drawable.ic_24px_route_point_a
                1 -> return R.drawable.ic_24px_route_point_b
                2 -> return R.drawable.ic_24px_route_point_c
            }
            throw AssertionError("Unknown intermediate point index: $index")
        }
    }
}