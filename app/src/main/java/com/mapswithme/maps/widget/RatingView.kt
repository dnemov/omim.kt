package com.mapswithme.maps.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextUtils.TruncateAt
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import com.mapswithme.maps.R
import com.mapswithme.maps.ugc.Impress

class RatingView : View {
    private var mDrawable: Drawable? = null
    private val mDrawableBounds = Rect()
    private val mBackgroundBounds = RectF()
    private val mTextBounds = Rect()
    private val mBackgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var mRating: String? = null
    @ColorInt
    private var mTextColor = 0
    private var mDrawSmile = false
    private var mBackgroundCornerRadius = 0
    private var mForceDrawBg = false
    private var mAlpha = 0
    private var mTruncate = TruncateAt.END
    @ColorInt
    private var mBgColor = 0

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

    private fun init(attrs: AttributeSet?) {
        mBackgroundCornerRadius =
            resources.getDimensionPixelSize(R.dimen.rating_view_background_radius)
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.RatingView
        )
        val textSize =
            a.getDimensionPixelSize(R.styleable.RatingView_android_textSize, 0).toFloat()
        mTextPaint.textSize = textSize
        mTextPaint.typeface = Typeface.create("Roboto", Typeface.BOLD)
        mRating = a.getString(R.styleable.RatingView_android_text)
        mDrawSmile = a.getBoolean(R.styleable.RatingView_drawSmile, true)
        mForceDrawBg = a.getBoolean(R.styleable.RatingView_forceDrawBg, true)
        mAlpha = a.getInteger(R.styleable.RatingView_android_alpha, DEF_ALPHA)
        val rating = a.getInteger(R.styleable.RatingView_rating, 0)
        val index = a.getInteger(
            R.styleable.RatingView_android_ellipsize,
            TruncateAt.END.ordinal
        )
        mTruncate = TruncateAt.values()[index]
        a.recycle()
        val r =
            Impress.values()[rating]
        setRating(r, mRating)
    }

    fun setRating(impress: Impress, rating: String?) {
        mRating = rating
        val res = context.resources
        mTextColor = res.getColor(impress.textColor)
        mBgColor = res.getColor(impress.bgColor)
        mBackgroundPaint.color = mBgColor
        mBackgroundPaint.alpha = mAlpha
        if (mDrawSmile) mDrawable = DrawableCompat.wrap(res.getDrawable(impress.drawableId))
        mTextPaint.color = mTextColor
        invalidate()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var height =
            getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        var width = paddingLeft
        if (mDrawable != null) {
            val drawableWidth = height - paddingTop - paddingBottom
            mDrawableBounds[paddingLeft, paddingTop, drawableWidth + paddingLeft] =
                drawableWidth + paddingTop
            width += drawableWidth
        }
        if (mRating != null) {
            mTextPaint.getTextBounds(mRating, 0, mRating!!.length, mTextBounds)
            val paddingLeft = if (mDrawable != null) paddingLeft else 0
            width += paddingLeft
            val defaultWidth =
                getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
            val availableSpace = defaultWidth - width - paddingRight
            val textWidth = mTextPaint.measureText(mRating)
            if (textWidth > availableSpace) {
                mRating =
                    TextUtils.ellipsize(mRating, mTextPaint, availableSpace.toFloat(), mTruncate)
                        .toString()
                mTextPaint.getTextBounds(mRating, 0, mRating!!.length, mTextBounds)
                width += availableSpace
            } else {
                width += textWidth.toInt()
            }
            if (height == 0) height = paddingTop + mTextBounds.height() + paddingBottom
        }
        width += paddingRight
        mBackgroundBounds[0f, 0f, width.toFloat()] = height.toFloat()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (background == null && mDrawable != null || mForceDrawBg) {
            canvas.drawRoundRect(
                mBackgroundBounds,
                mBackgroundCornerRadius.toFloat(),
                mBackgroundCornerRadius.toFloat(),
                mBackgroundPaint
            )
        }
        if (mDrawable != null) {
            mDrawable!!.mutate()
            DrawableCompat.setTint(mDrawable!!, mTextColor)
            mDrawable!!.bounds = mDrawableBounds
            mDrawable!!.draw(canvas)
        }
        if (mRating != null) {
            val yPos =
                (canvas.height / 2 - (mTextPaint.descent() + mTextPaint.ascent()) / 2).toInt()
            val xPos =
                if (mDrawable != null) mDrawable!!.bounds.right + paddingLeft else paddingLeft
            canvas.drawText(mRating!!, xPos.toFloat(), yPos.toFloat(), mTextPaint)
        }
    }

    companion object {
        private const val DEF_ALPHA = 31 /* 12% */
    }
}