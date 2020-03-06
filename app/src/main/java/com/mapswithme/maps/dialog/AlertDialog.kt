package com.mapswithme.maps.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.util.UiUtils
import com.mapswithme.util.log.LoggerFactory

open class AlertDialog : BaseMwmDialogFragment() {
    private var mTargetCallback: AlertDialogCallback? = null
    private var mFragmentManagerStrategy: ResolveFragmentManagerStrategy =
        ChildFragmentManagerStrategy()
    private var mDialogViewStrategy: ResolveDialogViewStrategy = AlertDialogStrategy()
    fun show(parent: Fragment, tag: String?) {
        val fm = mFragmentManagerStrategy.resolve(parent)
        if (fm.findFragmentByTag(tag) != null) return
        showInternal(tag, fm)
    }

    fun show(activity: FragmentActivity, tag: String?) {
        val fm = mFragmentManagerStrategy.resolve(activity)
        if (fm.findFragmentByTag(tag) != null) return
        showInternal(tag, fm)
    }

    private fun showInternal(
        tag: String?,
        fm: FragmentManager
    ) {
        val transaction = fm.beginTransaction()
        transaction.add(this, tag)
        transaction.commitAllowingStateLoss()
    }

    @get:LayoutRes
    protected open val layoutId: Int
        protected get() {
            throw UnsupportedOperationException(
                "By default, you " +
                        "shouldn't implement this method." +
                        " AlertDialog.Builder will do everything by itself. " +
                        "But if you want to use this method, " +
                        "you'll have to implement it"
            )
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onAttachInternal()
        } catch (e: ClassCastException) {
            val logger =
                LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
            logger.w(
                AlertDialog::class.java.simpleName,
                "Caller doesn't implement AlertDialogCallback interface."
            )
        }
    }

    private fun onAttachInternal() {
        mTargetCallback =
            (if (parentFragment == null) targetFragment else parentFragment) as AlertDialogCallback?
        if (mTargetCallback != null) return
        if (activity !is AlertDialogCallback) return
        mTargetCallback = activity as AlertDialogCallback?
    }

    override fun onDetach() {
        super.onDetach()
        mTargetCallback = null
    }

    protected fun setTargetCallback(targetCallback: AlertDialogCallback?) {
        mTargetCallback = targetCallback
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments ?: throw IllegalArgumentException("Arguments must be non null!")
        initStrategies(args)
        return mDialogViewStrategy.createView(this, args)
    }

    private fun initStrategies(args: Bundle) {
        val fragManagerStrategyIndex = args.getInt(
            ARG_DIALOG_VIEW_STRATEGY_INDEX,
            INVALID_ID
        )
        mFragmentManagerStrategy =
            FragManagerStrategyType.values()[fragManagerStrategyIndex].value
        val dialogViewStrategyIndex = args.getInt(
            ARG_DIALOG_VIEW_STRATEGY_INDEX,
            INVALID_ID
        )
        mDialogViewStrategy = DialogViewStrategyType.values()[dialogViewStrategyIndex].value
    }

    private fun onPositiveClicked(which: Int) {
        if (mTargetCallback != null) mTargetCallback!!.onAlertDialogPositiveClick(
            arguments!!.getInt(
                ARG_REQ_CODE
            ), which
        )
        dismissAllowingStateLoss()
    }

    private fun onNegativeClicked(which: Int) {
        if (mTargetCallback != null) mTargetCallback!!.onAlertDialogNegativeClick(
            arguments!!.getInt(
                ARG_REQ_CODE
            ), which
        )
        dismissAllowingStateLoss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (mTargetCallback != null) mTargetCallback!!.onAlertDialogCancel(
            arguments!!.getInt(
                ARG_REQ_CODE
            )
        )
    }

    private fun setFragmentManagerStrategy(strategy: ResolveFragmentManagerStrategy) {
        mFragmentManagerStrategy = strategy
    }

    private fun setDialogViewStrategy(strategy: ResolveDialogViewStrategy) {
        mDialogViewStrategy = strategy
    }

    class Builder {
        var reqCode = 0
            private set
        @get:StringRes
        @StringRes
        var titleId = 0
            private set
        @get:StringRes
        @StringRes
        var messageId = 0
            private set
        @get:StringRes
        @StringRes
        var positiveBtnId = 0
            private set
        @get:StringRes
        @StringRes
        var negativeBtnId = INVALID_ID
            private set
        @get:DrawableRes
        @DrawableRes
        var imageResId = INVALID_ID
            private set
        var fragManagerStrategyType =
            FragManagerStrategyType.DEFAULT
            private set
        var dialogViewStrategyType = DialogViewStrategyType.DEFAULT
            private set
        var dialogFactory: DialogFactory = DefaultDialogFactory()
            private set
        var negativeBtnTextColor =
            INVALID_ID
            private set

        fun setReqCode(reqCode: Int): Builder {
            this.reqCode = reqCode
            return this
        }

        fun setNegativeBtnTextColor(negativeBtnTextColor: Int): Builder {
            this.negativeBtnTextColor = negativeBtnTextColor
            return this
        }

        fun setTitleId(@StringRes titleId: Int): Builder {
            this.titleId = titleId
            return this
        }

        fun setMessageId(@StringRes messageId: Int): Builder {
            this.messageId = messageId
            return this
        }

        fun setPositiveBtnId(@StringRes btnId: Int): Builder {
            positiveBtnId = btnId
            return this
        }

        fun setNegativeBtnId(@StringRes btnId: Int): Builder {
            negativeBtnId = btnId
            return this
        }

        fun setImageResId(@DrawableRes imageResId: Int): Builder {
            this.imageResId = imageResId
            return this
        }

        fun setFragManagerStrategyType(strategyType: FragManagerStrategyType): Builder {
            fragManagerStrategyType = strategyType
            return this
        }

        fun setDialogViewStrategyType(strategyType: DialogViewStrategyType): Builder {
            dialogViewStrategyType = strategyType
            return this
        }

        fun build(): AlertDialog {
            return createDialog(this)
        }

        fun setDialogFactory(dialogFactory: DialogFactory): Builder {
            this.dialogFactory = dialogFactory
            return this
        }

    }

    private class ChildFragmentManagerStrategy : ResolveFragmentManagerStrategy {
        override fun resolve(baseFragment: Fragment): FragmentManager {
            return baseFragment.childFragmentManager
        }

        override fun resolve(activity: FragmentActivity): FragmentManager {
            throw UnsupportedOperationException("Not supported here!")
        }
    }

    private class ActivityFragmentManagerStrategy : ResolveFragmentManagerStrategy {
        override fun resolve(baseFragment: Fragment): FragmentManager {
            return baseFragment.requireActivity().supportFragmentManager
        }

        override fun resolve(activity: FragmentActivity): FragmentManager {
            return activity.supportFragmentManager
        }
    }

    private class AlertDialogStrategy : ResolveDialogViewStrategy {
        override fun createView(
            instance: AlertDialog,
            args: Bundle
        ): Dialog {
            val titleId =
                args.getInt(ARG_TITLE_ID)
            val messageId =
                args.getInt(ARG_MESSAGE_ID)
            val positiveButtonId =
                args.getInt(ARG_POSITIVE_BUTTON_ID)
            val negativeButtonId =
                args.getInt(ARG_NEGATIVE_BUTTON_ID)
            val builder =
                DialogUtils.buildAlertDialog(instance.context!!, titleId, messageId)
            builder.setPositiveButton(
                positiveButtonId
            ) { dialog: DialogInterface?, which: Int ->
                instance.onPositiveClicked(
                    which
                )
            }
            if (negativeButtonId != INVALID_ID) builder.setNegativeButton(
                negativeButtonId
            ) { dialog: DialogInterface?, which: Int ->
                instance.onNegativeClicked(
                    which
                )
            }
            return builder.show()
        }
    }

    private class ConfirmationDialogStrategy : ResolveDialogViewStrategy {
        override fun createView(
            fragment: AlertDialog,
            args: Bundle
        ): Dialog {
            val appCompatDialog = AppCompatDialog(fragment.context)
            val inflater = LayoutInflater.from(fragment.context)
            val root = inflater.inflate(fragment.layoutId, null, false)
            val declineBtn = root.findViewById<TextView>(R.id.decline_btn)
            val declineBtnTextId =
                args.getInt(ARG_NEGATIVE_BUTTON_ID)
            if (declineBtnTextId != INVALID_ID) {
                declineBtn.setText(args.getInt(ARG_NEGATIVE_BUTTON_ID))
                declineBtn.setOnClickListener { v: View? ->
                    fragment.onNegativeClicked(
                        DialogInterface.BUTTON_NEGATIVE
                    )
                }
            } else {
                UiUtils.hide(declineBtn)
            }
            val acceptBtn = root.findViewById<TextView>(R.id.accept_btn)
            acceptBtn.setText(args.getInt(ARG_POSITIVE_BUTTON_ID))
            acceptBtn.setOnClickListener { v: View? ->
                fragment.onPositiveClicked(
                    DialogInterface.BUTTON_POSITIVE
                )
            }
            val descriptionView = root.findViewById<TextView>(R.id.description)
            descriptionView.setText(args.getInt(ARG_MESSAGE_ID))
            val titleView = root.findViewById<TextView>(R.id.title)
            titleView.setText(args.getInt(ARG_TITLE_ID))
            val imageView =
                root.findViewById<ImageView>(R.id.image)
            val imageResId =
                args.getInt(ARG_IMAGE_RES_ID)
            val hasImage =
                imageResId != INVALID_ID
            imageView.setImageDrawable(if (hasImage) fragment.resources.getDrawable(imageResId) else null)
            val negativeBtnTextColor =
                args.getInt(ARG_NEGATIVE_BTN_TEXT_COLOR_RES_ID)
            val hasNegativeBtnCustomColor =
                negativeBtnTextColor != INVALID_ID
            if (hasNegativeBtnCustomColor) declineBtn.setTextColor(
                fragment.resources.getColor(
                    negativeBtnTextColor
                )
            )
            UiUtils.showIf(hasImage, imageView)
            appCompatDialog.setContentView(root)
            return appCompatDialog
        }
    }

    enum class FragManagerStrategyType(val value: ResolveFragmentManagerStrategy) {
        DEFAULT(ChildFragmentManagerStrategy()), ACTIVITY_FRAGMENT_MANAGER(
            ActivityFragmentManagerStrategy()
        );

    }

    enum class DialogViewStrategyType(val value: ResolveDialogViewStrategy) {
        DEFAULT(AlertDialogStrategy()), CONFIRMATION_DIALOG(ConfirmationDialogStrategy());

    }

    companion object {
        private const val ARG_TITLE_ID = "arg_title_id"
        private const val ARG_MESSAGE_ID = "arg_message_id"
        private const val ARG_POSITIVE_BUTTON_ID = "arg_positive_button_id"
        private const val ARG_NEGATIVE_BUTTON_ID = "arg_negative_button_id"
        private const val ARG_IMAGE_RES_ID = "arg_image_res_id"
        private const val ARG_NEGATIVE_BTN_TEXT_COLOR_RES_ID =
            "arg_neg_btn_text_color_res_id"
        private const val ARG_REQ_CODE = "arg_req_code"
        private const val ARG_FRAGMENT_MANAGER_STRATEGY_INDEX =
            "arg_fragment_manager_strategy_index"
        private const val ARG_DIALOG_VIEW_STRATEGY_INDEX = "arg_dialog_view_strategy_index"
        private const val INVALID_ID = -1
        private fun createDialog(builder: Builder): AlertDialog {
            val args = Bundle()
            args.putInt(
                ARG_TITLE_ID,
                builder.titleId
            )
            args.putInt(
                ARG_MESSAGE_ID,
                builder.messageId
            )
            args.putInt(
                ARG_POSITIVE_BUTTON_ID,
                builder.positiveBtnId
            )
            args.putInt(
                ARG_NEGATIVE_BUTTON_ID,
                builder.negativeBtnId
            )
            args.putInt(
                ARG_REQ_CODE,
                builder.reqCode
            )
            args.putInt(
                ARG_IMAGE_RES_ID,
                builder.imageResId
            )
            args.putInt(
                ARG_NEGATIVE_BTN_TEXT_COLOR_RES_ID,
                builder.negativeBtnTextColor
            )
            val fragManagerStrategyType =
                builder.fragManagerStrategyType
            args.putInt(
                ARG_FRAGMENT_MANAGER_STRATEGY_INDEX,
                fragManagerStrategyType.ordinal
            )
            val dialogViewStrategyType = builder.dialogViewStrategyType
            args.putInt(
                ARG_DIALOG_VIEW_STRATEGY_INDEX,
                dialogViewStrategyType.ordinal
            )
            val dialog =
                builder.dialogFactory.createDialog()
            dialog.arguments = args
            dialog.setFragmentManagerStrategy(fragManagerStrategyType.value)
            dialog.setDialogViewStrategy(dialogViewStrategyType.value)
            return dialog
        }
    }
}