package com.mapswithme.maps.onboarding

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment

import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics

class IntroductionDialogFragment : BaseMwmDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = super.onCreateDialog(savedInstanceState)
        val content =
            View.inflate(activity, R.layout.fragment_welcome, null)
        res.setContentView(content)
        val factory = screenFactory
        val button = content.findViewById<TextView>(R.id.accept_btn)
        button.setText(factory.action)
        button.setOnClickListener { v: View? -> onAcceptClicked() }
        val image =
            content.findViewById<ImageView>(R.id.iv__image)
        image.setImageResource(factory.image)
        val title = content.findViewById<TextView>(R.id.tv__title)
        title.setText(factory.title)
        val subtitle = content.findViewById<TextView>(R.id.tv__subtitle1)
        subtitle.setText(factory.subtitle)
        UiUtils.hide(content, R.id.decline_btn)
        return res
    }

    private val screenFactory: IntroductionScreenFactory
        private get() {
            val args = argumentsOrThrow
            val dataIndex =
                args.getInt(ARG_INTRODUCTION_FACTORY)
            return IntroductionScreenFactory.values()[dataIndex]
        }

    private fun onAcceptClicked() {
        val deepLink =
            argumentsOrThrow.getString(ARG_DEEPLINK)
        if (TextUtils.isEmpty(deepLink)) throw AssertionError("Deeplink must non-empty within introduction fragment!")
        val factory = screenFactory
        factory.createButtonClickListener().onIntroductionButtonClick(requireActivity(), deepLink!!)
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.ONBOARDING_DEEPLINK_SCREEN_ACCEPT,
            Statistics.params().add(
                Statistics.EventParam.TYPE,
                factory.toStatisticValue()
            )
        )
        dismissAllowingStateLoss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.ONBOARDING_DEEPLINK_SCREEN_DECLINE,
            Statistics.params().add(
                Statistics.EventParam.TYPE,
                screenFactory.toStatisticValue()
            )
        )
    }

    companion object {
        private const val ARG_DEEPLINK = "arg_deeplink"
        private const val ARG_INTRODUCTION_FACTORY = "arg_introduction_factory"
        @kotlin.jvm.JvmStatic
        fun show(
            fm: FragmentManager, deepLink: String,
            factory: IntroductionScreenFactory
        ) {
            val args = Bundle()
            args.putString(ARG_DEEPLINK, deepLink)
            args.putInt(
                ARG_INTRODUCTION_FACTORY,
                factory.ordinal
            )
            val fragment = IntroductionDialogFragment()
            fragment.arguments = args
            fragment.show(fm, IntroductionDialogFragment::class.java.name)
            Statistics.INSTANCE.trackEvent(
                Statistics.EventName.ONBOARDING_DEEPLINK_SCREEN_SHOW,
                Statistics.params().add(
                    Statistics.EventParam.TYPE,
                    factory.toStatisticValue()
                )
            )
        }
    }
}