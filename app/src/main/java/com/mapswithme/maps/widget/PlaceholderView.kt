package com.mapswithme.maps.widget

import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils

class PlaceholderView : LinearLayout {
    private lateinit var mImage: ImageView
    private lateinit var mTitle: TextView
    private lateinit var mSubtitle: TextView
    private var mImgMaxHeight = 0
    private var mImgMinHeight = 0
    @DrawableRes
    private var mImgSrcDefault = 0
    @StringRes
    private var mTitleResIdDefault = 0
    @StringRes
    private var mSubtitleResIdDefault = 0

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?
    ) {
        val res = resources
        mImgMaxHeight = res.getDimensionPixelSize(R.dimen.placeholder_size)
        mImgMinHeight = res.getDimensionPixelSize(R.dimen.placeholder_size_small)
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.placeholder_image, this, true)
        inflater.inflate(R.layout.placeholder_title, this, true)
        inflater.inflate(R.layout.placeholder_subtitle, this, true)
        orientation = VERTICAL
        initDefaultValues(context, attrs)
    }

    private fun initDefaultValues(
        context: Context,
        attrs: AttributeSet?
    ) {
        var attrsArray: TypedArray? = null
        try {
            attrsArray =
                context.theme.obtainStyledAttributes(attrs, R.styleable.PlaceholderView, 0, 0)
            mImgSrcDefault = attrsArray.getResourceId(
                R.styleable.PlaceholderView_imgSrcDefault,
                UiUtils.NO_ID
            )
            mTitleResIdDefault = attrsArray.getResourceId(
                R.styleable.PlaceholderView_titleDefault,
                UiUtils.NO_ID
            )
            mSubtitleResIdDefault = attrsArray.getResourceId(
                R.styleable.PlaceholderView_subTitleDefault,
                UiUtils.NO_ID
            )
        } finally {
            attrsArray?.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mImage = findViewById(R.id.image)
        mTitle = findViewById(R.id.title)
        mSubtitle = findViewById(R.id.subtitle)
        setupDefaultContent()
    }

    private fun setupDefaultContent() {
        if (isDefaultValueValid(mImgSrcDefault)) {
            mImage.setImageResource(mImgSrcDefault)
        }
        if (isDefaultValueValid(mTitleResIdDefault)) {
            mTitle.setText(mTitleResIdDefault)
        }
        if (isDefaultValueValid(mSubtitleResIdDefault)) {
            mSubtitle.setText(mSubtitleResIdDefault)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childrenTextTotalHeight =
            calcTotalTextChildrenHeight(widthMeasureSpec, heightMeasureSpec)
        val defHeight =
            View.getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val imgParams = mImage.layoutParams as MarginLayoutParams
        val potentialHeight =
            defHeight - paddingBottom - paddingTop - childrenTextTotalHeight -
                    imgParams.bottomMargin - imgParams.topMargin
        val imgSpaceRaw = Math.min(potentialHeight, mImgMaxHeight)
        imgParams.height = imgSpaceRaw
        imgParams.width = imgSpaceRaw
        measureChildWithMargins(mImage, widthMeasureSpec, 0, heightMeasureSpec, 0)
        val isImageSpaceAllowed = imgSpaceRaw > mImgMinHeight
        var childrenTotalHeight = childrenTextTotalHeight
        if (isImageSpaceAllowed) childrenTotalHeight += calcHeightWithMargins(
            mImage
        )
        UiUtils.showIf(isImageSpaceAllowed, mImage)
        val height = childrenTotalHeight + paddingTop + paddingBottom
        val width =
            View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private fun calcTotalTextChildrenHeight(widthMeasureSpec: Int, heightMeasureSpec: Int): Int {
        var totalHeight = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == View.VISIBLE && child !== mImage) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                totalHeight += calcHeightWithMargins(child)
            }
        }
        return totalHeight
    }

    fun setContent(
        @DrawableRes imageRes: Int, @StringRes titleRes: Int,
        @StringRes subtitleRes: Int
    ) {
        mImage.setImageResource(imageRes)
        mTitle.setText(titleRes)
        mSubtitle.setText(subtitleRes)
    }

    companion object {
        private fun isDefaultValueValid(defaultResId: Int): Boolean {
            return defaultResId != UiUtils.NO_ID
        }

        private fun calcHeightWithMargins(view: View): Int {
            val params = view.layoutParams as MarginLayoutParams
            return view.measuredHeight + params.bottomMargin + params.topMargin
        }
    }
}