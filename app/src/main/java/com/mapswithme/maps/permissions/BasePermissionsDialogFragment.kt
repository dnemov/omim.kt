package com.mapswithme.maps.permissions

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment

import com.mapswithme.util.PermissionsUtils
import com.mapswithme.util.log.LoggerFactory

abstract class BasePermissionsDialogFragment : BaseMwmDialogFragment(),
    View.OnClickListener {
    protected var requestCode = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) requestCode =
            args.getInt(ARG_REQUEST_CODE)
    }

    // We can't read actual theme, because permissions are not granted yet.
    override val customTheme: Int
        protected get() =// We can't read actual theme, because permissions are not granted yet.
            R.style.MwmTheme_DialogFragment_Fullscreen

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = super.onCreateDialog(savedInstanceState)
        res.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content =
            View.inflate(activity, layoutRes, null)
        res.setContentView(content)
        var button =
            content.findViewById<View>(firstActionButton)
        button?.setOnClickListener(this)
        button = content.findViewById(continueActionButton)
        button?.setOnClickListener { v: View? ->
            onContinueBtnClicked(
                v
            )
        }
        val image =
            content.findViewById<View>(R.id.iv__image) as ImageView
        image?.setImageResource(imageRes)
        val title = content.findViewById<View>(R.id.tv__title) as TextView
        title?.setText(titleRes)
        val subtitle =
            content.findViewById<View>(R.id.tv__subtitle1) as TextView
        subtitle?.setText(subtitleRes)
        return res
    }

    protected open fun onContinueBtnClicked(v: View?) {
        PermissionsUtils.requestPermissions(requireActivity(), requestCode)
    }

    @get:DrawableRes
    protected open val imageRes: Int
        protected get() = 0

    @get:StringRes
    protected open val titleRes: Int
        protected get() = 0

    @get:StringRes
    protected open val subtitleRes: Int
        protected get() = 0

    @get:LayoutRes
    protected abstract val layoutRes: Int

    @get:IdRes
    protected abstract val firstActionButton: Int

    protected abstract fun onFirstActionClick()
    @get:IdRes
    protected abstract val continueActionButton: Int

    override fun onClick(v: View) {
        if (v.id == firstActionButton) onFirstActionClick()
    }

    companion object {
        private val TAG = BasePermissionsDialogFragment::class.java.name
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private const val ARG_REQUEST_CODE = "arg_request_code"
        fun show(
            activity: FragmentActivity, requestCode: Int,
            dialogClass: Class<out BaseMwmDialogFragment?>
        ): DialogFragment? {
            val fm = activity.supportFragmentManager
            if (fm.isDestroyed) return null
            val f = fm.findFragmentByTag(dialogClass.name)
            if (f != null) return f as DialogFragment?
            var dialog: BaseMwmDialogFragment? = null
            try {
                dialog = dialogClass.newInstance()
                val args = Bundle()
                args.putInt(ARG_REQUEST_CODE, requestCode)
                dialog!!.arguments = args
                dialog.show(fm, dialogClass.name)
            } catch (e: java.lang.InstantiationException) {
                LOGGER.e(
                    TAG,
                    "Can't instantiate " + dialogClass.name + " fragment",
                    e
                )
            } catch (e: IllegalAccessException) {
                LOGGER.e(
                    TAG,
                    "Can't instantiate " + dialogClass.name + " fragment",
                    e
                )
            }
            return dialog
        }
    }
}