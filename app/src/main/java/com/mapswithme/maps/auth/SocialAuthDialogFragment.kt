package com.mapswithme.maps.auth

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.annotation.IdRes
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.AuthTokenType
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.maps.R
import com.mapswithme.maps.auth.SocialAuthDialogFragment
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.util.UiUtils
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics
import java.lang.ref.WeakReference
import java.util.*

class SocialAuthDialogFragment : BaseMwmDialogFragment() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val mFacebookCallbackManager = CallbackManager.Factory.create()
    private val mTokenHandlers = Arrays.asList(
        FacebookTokenHandler(), GoogleTokenHandler(), PhoneTokenHandler()
    )
    private var mCurrentTokenHandler: TokenHandler? = null
    private val mPhoneClickListener =
        View.OnClickListener { v: View? ->
            PhoneAuthActivity.Companion.startForResult(this)
        }
    private val mGoogleClickListener =
        View.OnClickListener {
            val intent = mGoogleSignInClient.signInIntent
            startActivityForResult(
                intent,
                Constants.REQ_CODE_GOOGLE_SIGN_IN
            )
        }
    private val mFacebookClickListener =
        View.OnClickListener { v: View? ->
            val lm = LoginManager.getInstance()
            lm.logInWithReadPermissions(
                this@SocialAuthDialogFragment,
                Constants.FACEBOOK_PERMISSIONS
            )
            lm.registerCallback(mFacebookCallbackManager, FBCallback(this@SocialAuthDialogFragment))
        }
    private lateinit var mPrivacyPolicyCheck: CheckBox
    private lateinit var mTermOfUseCheck: CheckBox
    private lateinit var mPromoCheck: CheckBox
    private var mTargetCallback: TargetFragmentCallback? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = super.onCreateDialog(savedInstanceState)
        res.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return res
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTargetCallback()
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(PrivateVariables.googleWebClientId())
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(activity!!, gso)
    }

    private fun setTargetCallback() {
        mTargetCallback = try {
            parentFragment as TargetFragmentCallback?
        } catch (e: ClassCastException) {
            throw ClassCastException("Caller must implement TargetFragmentCallback interface!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_auth_passport_dialog, container, false)
        setLoginButton(
            view,
            R.id.google_button,
            mGoogleClickListener
        )
        setLoginButton(
            view,
            R.id.facebook_button,
            mFacebookClickListener
        )
        setLoginButton(
            view,
            R.id.phone_button,
            mPhoneClickListener
        )
        mPromoCheck = view.findViewById(R.id.newsCheck)
        mPrivacyPolicyCheck = view.findViewById(R.id.privacyPolicyCheck)
        mPrivacyPolicyCheck.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            setButtonAvailability(
                view, isChecked && mTermOfUseCheck.isChecked,
                R.id.google_button, R.id.facebook_button, R.id.phone_button
            )
        }
        mTermOfUseCheck = view.findViewById(R.id.termOfUseCheck)
        mTermOfUseCheck.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            setButtonAvailability(
                view, isChecked && mPrivacyPolicyCheck.isChecked,
                R.id.google_button, R.id.facebook_button, R.id.phone_button
            )
        }
        UiUtils.linkifyView(
            view, R.id.privacyPolicyLink, R.string.sign_agree_pp_gdpr,
            Framework.nativeGetPrivacyPolicyLink()
        )
        UiUtils.linkifyView(
            view, R.id.termOfUseLink, R.string.sign_agree_tof_gdpr,
            Framework.nativeGetTermsOfUseLink()
        )
        setButtonAvailability(
            view, false, R.id.google_button, R.id.facebook_button,
            R.id.phone_button
        )
        return view
    }

    override fun onResume() {
        super.onResume()
        Statistics.INSTANCE.trackEvent(Statistics.EventName.UGC_AUTH_SHOWN)
    }

    private fun sendResult(
        resultCode: Int, socialToken: String?,
        @AuthTokenType type: Int, error: String?,
        isCancel: Boolean
    ) {
        if (mTargetCallback == null || !mTargetCallback!!.isTargetAdded) return
        val data = Intent()
        data.putExtra(Constants.EXTRA_SOCIAL_TOKEN, socialToken)
        data.putExtra(Constants.EXTRA_TOKEN_TYPE, type)
        data.putExtra(Constants.EXTRA_AUTH_ERROR, error)
        data.putExtra(Constants.EXTRA_IS_CANCEL, isCancel)
        data.putExtra(
            Constants.EXTRA_PRIVACY_POLICY_ACCEPTED,
            mPrivacyPolicyCheck.isChecked
        )
        data.putExtra(
            Constants.EXTRA_TERMS_OF_USE_ACCEPTED,
            mTermOfUseCheck.isChecked
        )
        data.putExtra(
            Constants.EXTRA_PROMO_ACCEPTED,
            mPromoCheck.isChecked
        )
        mTargetCallback!!.onTargetFragmentResult(resultCode, data)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return
        for (handler in mTokenHandlers) {
            if (handler.checkToken(requestCode, data)) {
                mCurrentTokenHandler = handler
                break
            }
        }
        if (mCurrentTokenHandler == null) return
        dismissAllowingStateLoss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        val resultCode: Int
        val token: String?
        @AuthTokenType val type: Int
        if (mCurrentTokenHandler == null) {
            resultCode = Activity.RESULT_CANCELED
            token = null
            type = Framework.SOCIAL_TOKEN_INVALID
        } else {
            resultCode = Activity.RESULT_OK
            token = mCurrentTokenHandler!!.token
            type = mCurrentTokenHandler!!.type
            if (TextUtils.isEmpty(token)) throw AssertionError("Token must be non-null while token handler is non-null!")
            if (type == Framework.SOCIAL_TOKEN_INVALID) throw AssertionError("Token type must be non-invalid while token handler is non-null!")
        }
        sendResult(resultCode, token, type, null, true)
        super.onDismiss(dialog)
    }

    private class FBCallback(fragment: SocialAuthDialogFragment) :
        FacebookCallback<LoginResult?> {
        private val mFragmentRef: WeakReference<SocialAuthDialogFragment>
        override fun onSuccess(loginResult: LoginResult?) {
            Statistics.INSTANCE.trackUGCExternalAuthSucceed(Statistics.ParamValue.FACEBOOK)
            LOGGER.d(
                TAG,
                "onSuccess"
            )
        }

        override fun onCancel() {
            LOGGER.w(
                TAG,
                "onCancel"
            )
            sendEmptyResult(
                Activity.RESULT_CANCELED, Framework.SOCIAL_TOKEN_FACEBOOK,
                null, true
            )
        }

        override fun onError(error: FacebookException) {
            LOGGER.e(
                TAG,
                "onError",
                error
            )
            sendEmptyResult(
                Activity.RESULT_CANCELED, Framework.SOCIAL_TOKEN_FACEBOOK,
                error?.message, false
            )
        }

        private fun sendEmptyResult(
            resultCode: Int, @AuthTokenType type: Int,
            error: String?, isCancel: Boolean
        ) {
            val fragment = mFragmentRef.get() ?: return
            fragment.sendResult(resultCode, null, type, error, isCancel)
        }

        init {
            mFragmentRef = WeakReference(fragment)
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = SocialAuthDialogFragment::class.java.simpleName
        private fun setLoginButton(
            root: View, @IdRes id: Int,
            clickListener: View.OnClickListener
        ) {
            val button = root.findViewById<View>(id)
            button.setOnClickListener(clickListener)
        }

        private fun setButtonAvailability(
            root: View,
            available: Boolean, @IdRes vararg ids: Int
        ) {
            for (id in ids) {
                val button = root.findViewById<View>(id)
                button.isEnabled = available
            }
        }
    }
}