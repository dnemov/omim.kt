package com.mapswithme.maps.widget

import android.content.Context
import android.graphics.Canvas
import android.text.TextUtils
import android.util.AttributeSet
import androidx.core.view.ViewCompat
import com.google.android.material.textfield.TextInputLayout

/**
 * Fixes bug mentioned here https://code.google.com/p/android/issues/detail?id=175228
 */
class CustomTextInputLayout : TextInputLayout {
    private var mHintChanged = true

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!,
        attrs
    ) {
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mHintChanged && ViewCompat.isLaidOut(this)) { // In case that hint is changed programmatically
            val currentEditTextHint = editText!!.hint
            if (!TextUtils.isEmpty(currentEditTextHint)) hint = currentEditTextHint
            mHintChanged = false
        }
    }

    override fun setHint(hint: CharSequence?) {
        super.setHint(hint)
        mHintChanged = true
    }
}