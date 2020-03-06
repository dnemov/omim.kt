package com.mapswithme.maps.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import android.widget.TextView
import com.mapswithme.maps.R
import com.mapswithme.util.Graphics
import com.mapswithme.util.UiUtils

class BookmarkBackupView : LinearLayout {
    private lateinit var mHeader: View
    private lateinit var mContentLayout: View
    private lateinit var mTitle: TextView
    private val mHeaderClickListener =
        OnClickListener { v: View? -> onHeaderClick() }
    private var mExpanded = true

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.item_bookmark_backup, this)
        mHeader = findViewById(R.id.header)
        mContentLayout = findViewById(R.id.content)
        UiUtils.showIf(mExpanded, mContentLayout)
        mTitle = mHeader.findViewById(R.id.title)
        mTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, toggle, null)
        mHeader.setOnClickListener(mHeaderClickListener)
    }

    private fun onHeaderClick() {
        mExpanded = !mExpanded
        val animator =
            if (mExpanded) createFadeInAnimator() else createFadeOutAnimator()
        animator.duration = ANIMATION_DURATION.toLong()
        animator.start()
        mTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, toggle, null)
    }

    private fun createFadeInAnimator(): Animator {
        val animator = ObjectAnimator.ofFloat(mContentLayout, "alpha", 0f, 1f)
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                UiUtils.show(mContentLayout)
                mHeader.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animator) {
                mHeader.isEnabled = true
            }
        })
        return animator
    }

    private fun createFadeOutAnimator(): Animator {
        val animator = ObjectAnimator.ofFloat(mContentLayout, "alpha", 1f, 0f)
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                mHeader.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animator) {
                UiUtils.hide(mContentLayout)
                mHeader.isEnabled = true
            }
        })
        return animator
    }

    private val toggle: Drawable
        get() = Graphics.tint(
            context,
            if (mExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
            R.attr.secondary
        )!!

    var expanded: Boolean
        get() = mExpanded
        set(expanded) {
            if (mExpanded == expanded) return
            mExpanded = expanded
            UiUtils.showIf(mExpanded, mContentLayout)
            mTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, toggle, null)
        }

    fun setMessage(msg: String) {
        val message = mContentLayout.findViewById<TextView>(R.id.message)
        message.text = msg
    }

    fun setBackupButtonLabel(label: String) {
        val button = mContentLayout.findViewById<TextView>(R.id.backup_button)
        button.text = label
    }

    fun hideBackupButton() {
        UiUtils.hide(mContentLayout, R.id.backup_button)
    }

    fun showBackupButton() {
        UiUtils.show(mContentLayout, R.id.backup_button)
    }

    fun hideRestoreButton() {
        UiUtils.hide(mContentLayout, R.id.restore_button)
    }

    fun showRestoreButton() {
        UiUtils.show(mContentLayout, R.id.restore_button)
    }

    fun hideProgressBar() {
        UiUtils.hide(mContentLayout, R.id.progress)
    }

    fun showProgressBar() {
        UiUtils.show(mContentLayout, R.id.progress)
    }

    fun setBackupClickListener(listener: OnClickListener?) {
        mContentLayout.findViewById<View>(R.id.backup_button)
            .setOnClickListener(listener)
    }

    fun setRestoreClickListener(listener: OnClickListener?) {
        mContentLayout.findViewById<View>(R.id.restore_button)
            .setOnClickListener(listener)
    }

    companion object {
        private const val ANIMATION_DURATION = 300
    }
}