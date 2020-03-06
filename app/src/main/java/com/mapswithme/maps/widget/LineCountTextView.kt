package com.mapswithme.maps.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.TextView

class LineCountTextView : TextView {
    interface OnLineCountCalculatedListener {
        fun onLineCountCalculated(grater: Boolean)
    }

    private var mListener: OnLineCountCalculatedListener? = null

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = layout
        if (layout != null) {
            val textHeight = layout.height
            val viewHeight = height
            if (mListener != null) {
                mListener!!.onLineCountCalculated(textHeight > viewHeight)
            }
        }
    }

    fun setListener(listener: OnLineCountCalculatedListener?) {
        mListener = listener
    }
}