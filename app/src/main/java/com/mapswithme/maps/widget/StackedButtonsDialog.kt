package com.mapswithme.maps.widget

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDialog
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils

class StackedButtonsDialog private constructor(
    context: Context, private val mTitle: String?, private val mMessage: String?,
    private val mPositive: String?, private val mPositiveListener: DialogInterface.OnClickListener?,
    private val mNeutral: String?, private val mNeutralListener: DialogInterface.OnClickListener?,
    private val mNegative: String?, private val mNegativeListener: DialogInterface.OnClickListener?,
    private val mCancelable: Boolean, private val mCancelListener: DialogInterface.OnCancelListener?
) : AppCompatDialog(context), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(mCancelable)
        setOnCancelListener(mCancelListener)
        setContentView(R.layout.dialog_stacked_buttons)
        val title = findViewById<View>(R.id.tv__title) as TextView?
        UiUtils.setTextAndHideIfEmpty(title, mTitle)
        val message = findViewById<View>(R.id.tv__message) as TextView?
        UiUtils.setTextAndHideIfEmpty(message, mMessage)
        val positive = findViewById<View>(R.id.btn__positive) as TextView?
        positive!!.setOnClickListener(this)
        UiUtils.setTextAndHideIfEmpty(positive, mPositive)
        val neutral = findViewById<View>(R.id.btn__neutral) as TextView?
        neutral!!.setOnClickListener(this)
        UiUtils.setTextAndHideIfEmpty(neutral, mNeutral)
        val negative = findViewById<View>(R.id.btn__negative) as TextView?
        negative!!.setOnClickListener(this)
        UiUtils.setTextAndHideIfEmpty(negative, mNegative)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn__positive -> {
                mPositiveListener?.onClick(
                    this,
                    DialogInterface.BUTTON_POSITIVE
                )
                dismiss()
            }
            R.id.btn__neutral -> {
                mNeutralListener?.onClick(
                    this,
                    DialogInterface.BUTTON_NEUTRAL
                )
                dismiss()
            }
            R.id.btn__negative -> {
                mNegativeListener?.onClick(
                    this,
                    DialogInterface.BUTTON_NEGATIVE
                )
                dismiss()
            }
        }
    }

    class Builder(private val mContext: Context) {
        private var mTitle: String?
        private var mMessage: String? = null
        private var mPositive: String?
        private var mPositiveListener: DialogInterface.OnClickListener? = null
        private var mNeutral: String? = null
        private var mNeutralListener: DialogInterface.OnClickListener? = null
        private var mNegative: String?
        private var mNegativeListener: DialogInterface.OnClickListener? = null
        private var mCancelable = true
        private var mCancelListener: DialogInterface.OnCancelListener? = null
        fun setTitle(@StringRes titleId: Int): Builder {
            mTitle = mContext.getString(titleId)
            return this
        }

        fun setMessage(@StringRes messageId: Int): Builder {
            mMessage = mContext.getString(messageId)
            return this
        }

        fun setPositiveButton(
            @StringRes resId: Int,
            listener: DialogInterface.OnClickListener?
        ): Builder {
            mPositive = mContext.getString(resId)
            mPositiveListener = listener
            return this
        }

        fun setNeutralButton(
            @StringRes resId: Int,
            listener: DialogInterface.OnClickListener?
        ): Builder {
            mNeutral = mContext.getString(resId)
            mNeutralListener = listener
            return this
        }

        fun setNegativeButton(
            @StringRes resId: Int,
            listener: DialogInterface.OnClickListener?
        ): Builder {
            mNegative = mContext.getString(resId)
            mNegativeListener = listener
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            mCancelable = cancelable
            return this
        }

        fun setCancelListener(listener: DialogInterface.OnCancelListener?): Builder {
            mCancelListener = listener
            return this
        }

        fun build(): StackedButtonsDialog {
            return StackedButtonsDialog(
                mContext, mTitle, mMessage, mPositive, mPositiveListener,
                mNeutral, mNeutralListener, mNegative, mNegativeListener,
                mCancelable, mCancelListener
            )
        }

        init {
            mTitle = mContext.getString(android.R.string.dialog_alert_title)
            mPositive = mContext.getString(android.R.string.ok)
            mNegative = mContext.getString(android.R.string.no)
        }
    }

}