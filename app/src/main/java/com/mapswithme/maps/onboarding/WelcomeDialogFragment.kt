package com.mapswithme.maps.onboarding

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.news.OnboardingStep
import com.mapswithme.maps.onboarding.BaseNewsFragment.NewsDialogListener
import com.mapswithme.util.Counters
import com.mapswithme.util.SharedPropertiesUtils
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics
import java.util.*

class WelcomeDialogFragment : BaseMwmDialogFragment(),
    View.OnClickListener {
    private val mOnboardingSteps =
        Stack<OnboardingStep>()
    private var mPolicyAgreementListener: PolicyAgreementListener? = null
    private var mOnboardingStepPassedListener: OnboardingStepPassedListener? = null
    private var mOnboardinStep: OnboardingStep? = null
    private lateinit var mContentView: View
    private lateinit var mImage: ImageView
    private lateinit var mTitle: TextView
    private lateinit var mSubtitle: TextView
    private lateinit var mAcceptBtn: TextView
    private lateinit var mTermOfUseCheckbox: CheckBox
    private lateinit var mPrivacyPolicyCheckbox: CheckBox
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity is NewsDialogListener) mPolicyAgreementListener =
            activity as PolicyAgreementListener
        if (activity is OnboardingStepPassedListener) mOnboardingStepPassedListener =
            activity
    }

    override fun onDetach() {
        mPolicyAgreementListener = null
        super.onDetach()
    }

    override val customTheme: Int
        protected get() = if (ThemeUtils.isNightTheme) R.style.MwmTheme_DialogFragment_NoFullscreen_Night else R.style.MwmTheme_DialogFragment_NoFullscreen

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = super.onCreateDialog(savedInstanceState)
        res.requestWindowFeature(Window.FEATURE_NO_TITLE)
        res.setCancelable(false)
        hanldeOnboardingSteps()
        mContentView = View.inflate(activity, R.layout.fragment_welcome, null)
        res.setContentView(mContentView)
        mAcceptBtn = mContentView.findViewById(R.id.accept_btn)
        mAcceptBtn.setOnClickListener(this)
        mImage = mContentView.findViewById(R.id.iv__image)
        mImage.setImageResource(R.drawable.img_welcome)
        mTitle = mContentView.findViewById(R.id.tv__title)
        val headers =
            Arrays.asList(
                getString(R.string.new_onboarding_step1_header),
                getString(R.string.new_onboarding_step1_header_2)
            )
        val titleText = TextUtils.join(UiUtils.NEW_STRING_DELIMITER, headers)
        mTitle.text = titleText
        mSubtitle = mContentView.findViewById(R.id.tv__subtitle1)
        mSubtitle.setText(R.string.sign_message_gdpr)
        initUserAgreementViews()
        bindWelcomeScreenType()
        if (savedInstanceState == null) trackStatisticEvent(Statistics.EventName.ONBOARDING_SCREEN_SHOW)
        return res
    }

    private fun hanldeOnboardingSteps() {
        val args = arguments
        if (args != null) {
            val hasManySteps =
                args.containsKey(ARG_HAS_MANY_STEPS)
            if (hasManySteps) {
                mOnboardingSteps.push(OnboardingStep.SHARE_EMOTIONS)
                mOnboardingSteps.push(OnboardingStep.EXPERIENCE)
                mOnboardingSteps.push(OnboardingStep.DREAM_AND_PLAN)
            }
            val hasSpecificStep =
                args.containsKey(ARG_SPECIFIC_STEP)
            if (hasSpecificStep) mOnboardinStep =
                OnboardingStep.values()[args.getInt(ARG_SPECIFIC_STEP)]
            if (hasManySteps && hasSpecificStep) {
                var step: OnboardingStep? = null
                while (mOnboardinStep != step) {
                    step = mOnboardingSteps.pop()
                }
                mOnboardinStep = step
                return
            }
            if (hasManySteps) mOnboardinStep = mOnboardingSteps.pop()
        }
    }

    private fun initUserAgreementViews() {
        mTermOfUseCheckbox = mContentView.findViewById(R.id.term_of_use_welcome_checkbox)
        mTermOfUseCheckbox.isChecked =
            SharedPropertiesUtils.isTermOfUseAgreementConfirmed(requireContext())
        mPrivacyPolicyCheckbox =
            mContentView.findViewById(R.id.privacy_policy_welcome_checkbox)
        mPrivacyPolicyCheckbox.isChecked =
            SharedPropertiesUtils.isPrivacyPolicyAgreementConfirmed(requireContext())
        mTermOfUseCheckbox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            onTermsOfUseViewChanged(
                isChecked
            )
        }
        mPrivacyPolicyCheckbox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            onPrivacyPolicyViewChanged(
                isChecked
            )
        }
        UiUtils.linkifyView(
            mContentView, R.id.privacy_policy_welcome,
            R.string.sign_agree_pp_gdpr, Framework.nativeGetPrivacyPolicyLink()
        )
        UiUtils.linkifyView(
            mContentView, R.id.term_of_use_welcome,
            R.string.sign_agree_tof_gdpr, Framework.nativeGetTermsOfUseLink()
        )
    }

    private fun onPrivacyPolicyViewChanged(isChecked: Boolean) {
        SharedPropertiesUtils.putPrivacyPolicyAgreement(requireContext(), isChecked)
        onCheckedValueChanged(isChecked, mTermOfUseCheckbox.isChecked)
    }

    private fun onTermsOfUseViewChanged(isChecked: Boolean) {
        SharedPropertiesUtils.putTermOfUseAgreement(requireContext(), isChecked)
        onCheckedValueChanged(isChecked, mPrivacyPolicyCheckbox.isChecked)
    }

    private fun onCheckedValueChanged(
        isChecked: Boolean,
        isAnotherConditionChecked: Boolean
    ) {
        val isAgreementGranted = isChecked && isAnotherConditionChecked
        if (!isAgreementGranted) return
        trackStatisticEvent(Statistics.EventName.ONBOARDING_SCREEN_ACCEPT)
        if (mPolicyAgreementListener != null) mPolicyAgreementListener!!.onPolicyAgreementApplied()
        dismissAllowingStateLoss()
    }

    private fun bindWelcomeScreenType() {
        val hasBindingType = mOnboardinStep != null
        UiUtils.showIf(hasBindingType, mContentView, R.id.button_container)
        val hasDeclineBtn = (hasBindingType
                && mOnboardinStep!!.hasDeclinedButton())
        val declineBtn = mContentView.findViewById<TextView>(R.id.decline_btn)
        UiUtils.showIf(hasDeclineBtn, declineBtn)
        val userAgreementBlock =
            mContentView.findViewById<View>(R.id.user_agreement_block)
        UiUtils.hideIf(hasBindingType, userAgreementBlock)
        if (hasDeclineBtn) declineBtn.setText(mOnboardinStep!!.declinedButtonResId)
        if (!hasBindingType) return
        mTitle.setText(mOnboardinStep!!.title)
        mImage.setImageResource(mOnboardinStep!!.image)
        mAcceptBtn.setText(mOnboardinStep!!.acceptButtonResId)
        declineBtn.setOnClickListener { v: View? -> onDeclineBtnClicked() }
        mSubtitle.setText(mOnboardinStep!!.subtitle)
    }

    private fun onDeclineBtnClicked() {
        Counters.setFirstStartDialogSeen(requireContext())
        trackStatisticEvent(Statistics.EventName.ONBOARDING_SCREEN_DECLINE)
        dismissAllowingStateLoss()
    }

    private fun trackStatisticEvent(event: String) {
        val value =
            if (mOnboardinStep == null) DEF_STATISTICS_VALUE else mOnboardinStep!!.toStatisticValue()!!
        val builder = Statistics
            .params().add(Statistics.EventParam.TYPE, value)
        Statistics.INSTANCE.trackEvent(event, builder)
    }

    override fun onClick(v: View) {
        if (v.id != R.id.accept_btn) return
        trackStatisticEvent(Statistics.EventName.ONBOARDING_SCREEN_ACCEPT)
        if (!mOnboardingSteps.isEmpty()) {
            mOnboardinStep = mOnboardingSteps.pop()
            if (mOnboardingStepPassedListener != null) mOnboardingStepPassedListener!!.onOnboardingStepPassed(
                mOnboardinStep!!
            )
            bindWelcomeScreenType()
            return
        }
        if (mOnboardinStep != null && mOnboardingStepPassedListener != null) mOnboardingStepPassedListener!!.onOnboardingStepPassed(
            mOnboardinStep!!
        )
        Counters.setFirstStartDialogSeen(requireContext())
        dismissAllowingStateLoss()
        if (mOnboardingStepPassedListener != null) mOnboardingStepPassedListener!!.onLastOnboardingStepPassed()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (!isAgreementDeclined(requireContext())) Counters.setFirstStartDialogSeen(
            requireContext()
        )
        if (mOnboardingStepPassedListener != null) mOnboardingStepPassedListener!!.onOnboardingStepCancelled()
    }

    interface PolicyAgreementListener {
        fun onPolicyAgreementApplied()
    }

    interface OnboardingStepPassedListener {
        fun onOnboardingStepPassed(step: OnboardingStep)
        fun onLastOnboardingStepPassed()
        fun onOnboardingStepCancelled()
    }

    companion object {
        private const val ARG_SPECIFIC_STEP = "arg_specific_step"
        private const val ARG_HAS_MANY_STEPS = "arg_has_many_steps"
        private const val DEF_STATISTICS_VALUE = "agreement"
        @kotlin.jvm.JvmStatic
        fun show(activity: FragmentActivity) {
            create(activity, null)
        }

        @kotlin.jvm.JvmStatic
        fun showOnboardinSteps(activity: FragmentActivity) {
            val args = Bundle()
            args.putBoolean(ARG_HAS_MANY_STEPS, true)
            create(activity, args)
        }

        @kotlin.jvm.JvmStatic
        fun showOnboardinStepsStartWith(
            activity: FragmentActivity,
            startStep: OnboardingStep
        ) {
            val args = Bundle()
            args.putBoolean(ARG_HAS_MANY_STEPS, true)
            args.putInt(ARG_SPECIFIC_STEP, startStep.ordinal)
            create(activity, args)
        }

        @kotlin.jvm.JvmStatic
        fun showOnboardinStep(
            activity: FragmentActivity,
            step: OnboardingStep
        ) {
            val args = Bundle()
            args.putInt(ARG_SPECIFIC_STEP, step.ordinal)
            create(activity, args)
        }

        @kotlin.jvm.JvmStatic
        fun isFirstLaunch(activity: FragmentActivity): Boolean {
            if (Counters.getFirstInstallVersion() < BuildConfig.VERSION_CODE) return false
            val fm = activity.supportFragmentManager
            return if (fm.isDestroyed) false else !Counters.isFirstStartDialogSeen(activity)
        }

        private fun create(activity: FragmentActivity, args: Bundle?) {
            val fragment = WelcomeDialogFragment()
            fragment.arguments = args
            activity.supportFragmentManager
                .beginTransaction()
                .add(fragment, WelcomeDialogFragment::class.java.name)
                .commitAllowingStateLoss()
        }

        @kotlin.jvm.JvmStatic
        fun find(activity: FragmentActivity): DialogFragment? {
            val fm = activity.supportFragmentManager
            if (fm.isDestroyed) return null
            val f =
                fm.findFragmentByTag(WelcomeDialogFragment::class.java.name)
            return f as DialogFragment?
        }

        @kotlin.jvm.JvmStatic
        fun isAgreementDeclined(context: Context): Boolean {
            return (!SharedPropertiesUtils.isTermOfUseAgreementConfirmed(context)
                    || !SharedPropertiesUtils.isPrivacyPolicyAgreementConfirmed(context))
        }
    }
}