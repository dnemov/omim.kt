package com.mapswithme.maps.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils

class SubscriptionButton : FrameLayout {
    private var mButtonBackground: Drawable? = null
    @ColorInt
    private var mButtonTextColor = 0
    private var mSaleBackground: Drawable? = null
    @ColorInt
    private var mSaleTextColor = 0
    @ColorInt
    private var mProgressColor = 0
    private lateinit var mSaleView: TextView
    private lateinit var mNameView: TextView
    private lateinit var mPriceView: TextView
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mButtonContainer: View
    private var mShowSale = false

    constructor(context: Context) : super(context) {}
    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.SubscriptionButton)
        try {
            mButtonBackground = a.getDrawable(R.styleable.SubscriptionButton_buttonBackground)
            mButtonTextColor = a.getColor(R.styleable.SubscriptionButton_buttonTextColor, 0)
            mProgressColor = a.getColor(R.styleable.SubscriptionButton_progressColor, 0)
            mSaleBackground = a.getDrawable(R.styleable.SubscriptionButton_saleBackground)
            mSaleTextColor = a.getColor(R.styleable.SubscriptionButton_saleTextColor, 0)
            mShowSale = a.getBoolean(R.styleable.SubscriptionButton_showSale, false)
            LayoutInflater.from(context).inflate(R.layout.subscription_button, this, true)
        } finally {
            a.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mButtonContainer = findViewById(R.id.button_container)
        mButtonContainer.background = mButtonBackground
        mNameView = mButtonContainer.findViewById(R.id.name)
        mNameView.setTextColor(mButtonTextColor)
        mPriceView = mButtonContainer.findViewById(R.id.price)
        mPriceView.setTextColor(mButtonTextColor)
        mProgressBar = mButtonContainer.findViewById(R.id.progress)
        setProgressBarColor()
        mSaleView = findViewById(R.id.sale)
        if (mShowSale) {
            UiUtils.show(mSaleView)
            mSaleView.background = mSaleBackground
            mSaleView.setTextColor(mSaleTextColor)
        } else {
            UiUtils.hide(mSaleView)
            val params =
                mButtonContainer.layoutParams as MarginLayoutParams
            params.topMargin = 0
        }
    }

    private fun setProgressBarColor() {
        if (Utils.isLollipopOrLater) mProgressBar.indeterminateTintList =
            ColorStateList.valueOf(mProgressColor) else mProgressBar.indeterminateDrawable.setColorFilter(
            mProgressColor,
            PorterDuff.Mode.SRC_IN
        )
    }

    fun setName(name: String) {
        mNameView.text = name
    }

    fun setPrice(price: String) {
        mPriceView.text = price
    }

    fun setSale(sale: String) {
        mSaleView.text = sale
    }

    fun showProgress() {
        UiUtils.hide(mNameView, mPriceView)
        UiUtils.show(mProgressBar)
    }

    fun hideProgress() {
        UiUtils.hide(mProgressBar)
        UiUtils.show(mNameView, mPriceView)
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        mButtonContainer.setOnClickListener(listener)
    }
}