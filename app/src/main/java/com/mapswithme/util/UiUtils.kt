package com.mapswithme.util

import android.animation.Animator
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R

object UiUtils {
    private val DEFAULT_TINT_COLOR = Color.parseColor("#20000000")
    const val NO_ID = -1
    val NEW_STRING_DELIMITER = "\n"
    val PHRASE_SEPARATOR = " • "
    val APPROXIMATE_SYMBOL = "~"
    private var sScreenDensity: Float = 0.toFloat()

    val isTablet: Boolean
        get() = MwmApplication.get().resources.getBoolean(R.bool.tabletLayout)

    fun addStatusBarOffset(view: View) {
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(0, UiUtils.getStatusBarHeight(view.context), 0, 0)
    }

    fun bringViewToFrontOf(frontView: View, backView: View) {
        if (Utils.isLollipopOrLater)
            frontView.z = backView.z + 1
        else
            frontView.bringToFront()
    }

    fun linkifyView(
        view: View, @IdRes id: Int, @StringRes stringId: Int,
        link: String
    ) {
        val policyView = view.findViewById<TextView>(id)
        val rs = policyView.resources
        policyView.text = Html.fromHtml(rs.getString(stringId, link))
        policyView.movementMethod = LinkMovementMethod.getInstance()
    }

    class SimpleAnimationListener : AnimationListener {
        override fun onAnimationStart(animation: Animation) {}

        override fun onAnimationEnd(animation: Animation) {}

        override fun onAnimationRepeat(animation: Animation) {}
    }

    open class SimpleAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}

        override fun onAnimationEnd(animation: Animator) {}

        override fun onAnimationCancel(animation: Animator) {}

        override fun onAnimationRepeat(animation: Animator) {}
    }

    interface OnViewMeasuredListener {
        fun onViewMeasured(width: Int, height: Int)
    }

    fun waitLayout(view: View, callback: ViewTreeObserver.OnGlobalLayoutListener) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // viewTreeObserver can be dead(isAlive() == false), we should get a new one here.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                    view.viewTreeObserver.removeGlobalOnLayoutListener(this)
                else
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                callback.onGlobalLayout()
            }
        })
    }

    fun measureView(frame: View, listener: OnViewMeasuredListener) {
        waitLayout(frame, ViewTreeObserver.OnGlobalLayoutListener {
            val ac = frame.context as Activity
            if (ac == null || ac.isFinishing)
                return@OnGlobalLayoutListener

            listener.onViewMeasured(frame.measuredWidth, frame.measuredHeight)
        })
    }

    fun hide(view: View?) {
        view?.visibility = View.GONE
    }

    @JvmStatic
    fun hide(vararg views: View?) {
        for (v in views)
            v?.visibility = View.GONE
    }

    /*fun hide(frame: View, @IdRes viewId: Int) {
        val view = frame.findViewById<View>(viewId) ?: return

        hide(view)
    }*/

    fun hide(frame: View?, @IdRes vararg viewIds: Int) {
        for (id in viewIds) {
            val view = frame?.findViewById<View>(id) ?: return
            hide(view)
        }
    }

    @JvmStatic
    fun show(view: View?) {
        view?.visibility = View.VISIBLE
    }

    fun show(vararg views: View?) {
        for (v in views)
            v?.visibility = View.VISIBLE
    }

    /*fun show(frame: View?, @IdRes viewId: Int) {
        val view = frame?.findViewById<View>(viewId) ?: return

        show(view)
    }*/

    fun show(frame: View?, @IdRes vararg viewIds: Int) {
        for (id in viewIds) {
            val view = frame?.findViewById<View>(id) ?: return
            show(view)
        }
    }

    fun invisible(view: View) {
        view.visibility = View.INVISIBLE
    }

    fun invisible(vararg views: View) {
        for (v in views)
            v.visibility = View.INVISIBLE
    }

    /*fun invisible(frame: View, @IdRes viewId: Int) {
        invisible(frame.findViewById(viewId))
    }*/

    fun invisible(frame: View, @IdRes vararg viewIds: Int) {
        for (id in viewIds) {
            invisible(frame.findViewById(id))
        }
    }

    fun isHidden(view: View?): Boolean {
        return view?.visibility == View.GONE
    }

    fun isInvisible(view: View?): Boolean {
        return view?.visibility == View.INVISIBLE
    }

    fun isVisible(view: View): Boolean {
        return view.visibility == View.VISIBLE
    }

    fun visibleIf(condition: Boolean, view: View?) {
        view?.visibility = if (condition) View.VISIBLE else View.INVISIBLE
    }

    fun visibleIf(condition: Boolean, vararg views: View) {
        if (condition)
            show(*views)
        else
            invisible(*views)
    }

    @JvmStatic
    fun showIf(condition: Boolean, view: View?) {
        view?.visibility = if (condition) View.VISIBLE else View.GONE
    }

    fun hideIf(condition: Boolean, vararg views: View) {
        if (condition) {
            hide(*views)
        } else {
            show(*views)
        }
    }

    fun showIf(condition: Boolean, vararg views: View?) {
        if (condition)
            show(*views)
        else
            hide(*views)
    }

    fun showIf(condition: Boolean, parent: View?, @IdRes vararg viewIds: Int) {
        for (id in viewIds) {
            if (condition)
                show(parent?.findViewById(id))
            else
                hide(parent?.findViewById(id))
        }
    }

    fun setTextAndShow(tv: TextView?, text: CharSequence?) {
        tv?.text = text
        show(tv)
    }

    fun setTextAndHideIfEmpty(tv: TextView?, text: CharSequence?) {
        tv?.text = text
        showIf(!TextUtils.isEmpty(text), tv)
    }

    fun clearTextAndHide(tv: TextView) {
        tv.text = ""
        hide(tv)
    }

    fun showHomeUpButton(toolbar: Toolbar) {
        toolbar.setNavigationIcon(ThemeUtils.getResource(toolbar.context, R.attr.homeAsUpIndicator))
    }

    fun deviceOrientationAsString(activity: Activity): String {
        var rotation =
            if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) "|" else "-"
        when (activity.windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> rotation += "0"
            Surface.ROTATION_90 -> rotation += "90"
            Surface.ROTATION_180 -> rotation += "180"
            Surface.ROTATION_270 -> rotation += "270"
        }
        return rotation
    }

    fun dimen(@DimenRes id: Int): Int {
        return dimen(MwmApplication.get(), id)
    }

    fun dimen(context: Context, @DimenRes id: Int): Int {
        return context.resources.getDimensionPixelSize(id)
    }

    fun toPx(dp: Int): Int {
        if (sScreenDensity == 0f)
            sScreenDensity = MwmApplication.get().resources.displayMetrics.density

        return (dp * sScreenDensity + 0.5).toInt()
    }

    fun updateButton(button: Button) {
        button.setTextColor(
            ThemeUtils.getColor(
                button.context, if (button.isEnabled)
                    R.attr.buttonTextColor
                else
                    R.attr.buttonTextColorDisabled
            )
        )
    }

    fun updateRedButton(button: Button) {
        button.setTextColor(
            ThemeUtils.getColor(
                button.context, if (button.isEnabled)
                    R.attr.redButtonTextColor
                else
                    R.attr.redButtonTextColorDisabled
            )
        )
    }

    fun getUriToResId(context: Context, @AnyRes resId: Int): Uri {
        val resources = context.resources
        return Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                    + resources.getResourcePackageName(resId) + '/'.toString()
                    + resources.getResourceTypeName(resId) + '/'.toString()
                    + resources.getResourceEntryName(resId)
        )
    }

    fun setInputError(layout: TextInputLayout, @StringRes error: Int) {
        layout.error = if (error == 0) null else layout.context.getString(error)
        layout.editText!!.setTextColor(
            if (error == 0)
                ThemeUtils.getColor(layout.context, android.R.attr.textColorPrimary)
            else
                layout.context.resources.getColor(R.color.base_red)
        )
    }

    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun isViewTouched(event: MotionEvent, view: View): Boolean {
        if (UiUtils.isHidden(view))
            return false

        val x = event.x.toInt()
        val y = event.y.toInt()
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewX = location[0]
        val viewY = location[1]
        val width = view.width
        val height = view.height
        val viewRect = Rect(viewX, viewY, viewX + width, viewY + height)

        return viewRect.contains(x, y)
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val res = context.resources
        val resourceId = res.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0)
            result = res.getDimensionPixelSize(resourceId)

        return result
    }

    fun extendViewWithStatusBar(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return

        val statusBarHeight = getStatusBarHeight(view.context)
        val lp = view.layoutParams
        lp.height += statusBarHeight
        view.layoutParams = lp
        extendViewPaddingTop(view, statusBarHeight)
    }

    fun extendViewPaddingWithStatusBar(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return

        val statusBarHeight = getStatusBarHeight(view.context)
        extendViewPaddingTop(view, statusBarHeight)
    }

    private fun extendViewPaddingTop(view: View, statusBarHeight: Int) {
        view.setPadding(
            view.paddingLeft, view.paddingTop + statusBarHeight,
            view.paddingRight, view.paddingBottom
        )
    }

    fun extendViewMarginWithStatusBar(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return

        val statusBarHeight = getStatusBarHeight(view.context)
        val lp = view.layoutParams as ViewGroup.MarginLayoutParams
        val margin = lp.marginStart
        lp.setMargins(margin, margin + statusBarHeight, margin, margin)
        view.layoutParams = lp
    }

    fun setupStatusBar(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        val statusBarTintView = View(activity)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity))
        params.gravity = Gravity.TOP
        statusBarTintView.layoutParams = params
        statusBarTintView.setBackgroundColor(DEFAULT_TINT_COLOR)
        statusBarTintView.visibility = View.VISIBLE
        val decorViewGroup = activity.window.decorView as ViewGroup
        decorViewGroup.addView(statusBarTintView)
    }

    fun setupColorStatusBar(activity: Activity, @ColorRes statusBarColor: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return

        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(activity, statusBarColor)
    }

    fun getCompassYOffset(context: Context): Int {
        return getStatusBarHeight(context)
    }

    @AnyRes
    fun getStyledResourceId(context: Context, @AttrRes res: Int): Int {
        var a: TypedArray? = null
        try {
            a = context.obtainStyledAttributes(intArrayOf(res))
            return a!!.getResourceId(0, NO_ID)
        } finally {
            a?.recycle()
        }
    }

    fun setBackgroundDrawable(view: View, @AttrRes res: Int) {
        view.setBackgroundResource(getStyledResourceId(view.context, res))
    }

    fun expandTouchAreaForView(view: View, extraArea: Int) {
        val parent = view.parent as View
        parent.post {
            val rect = Rect()
            view.getHitRect(rect)
            rect.top -= extraArea
            rect.left -= extraArea
            rect.right += extraArea
            rect.bottom += extraArea
            parent.touchDelegate = TouchDelegate(rect, view)
        }
    }

    fun expandTouchAreaForViews(extraArea: Int, vararg views: View) {
        for (view in views)
            expandTouchAreaForView(view, extraArea)
    }

    fun expandTouchAreaForView(
        view: View, top: Int, left: Int,
        bottom: Int, right: Int
    ) {
        val parent = view.parent as View
        parent.post {
            val rect = Rect()
            view.getHitRect(rect)
            rect.top -= top
            rect.left -= left
            rect.right += right
            rect.bottom += bottom
            parent.touchDelegate = TouchDelegate(rect, view)
        }
    }

    @ColorInt
    fun getNotificationColor(context: Context): Int {
        return context.resources.getColor(R.color.notification)
    }
}// utility class
