package com.mapswithme.maps.ugc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.Framework.AuthTokenType
import com.mapswithme.maps.R
import com.mapswithme.maps.background.Notifier
import com.mapswithme.maps.background.Notifier.Companion.from
import com.mapswithme.maps.base.BaseToolbarAuthFragment
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.metrics.UserActionsLogger.logUgcSaved
import com.mapswithme.maps.ugc.UGC.Companion.setSaveListener
import com.mapswithme.maps.ugc.UGC.Companion.setUGCUpdate
import com.mapswithme.maps.ugc.UGC.SaveUGCListener
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Language
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.Statistics

class UGCEditorFragment : BaseToolbarAuthFragment() {
    private val mUGCRatingAdapter = UGCRatingAdapter()
    private lateinit var mReviewEditText: EditText
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_ugc_editor, container, false)
        mReviewEditText = root.findViewById(R.id.review)
        val rvRatingView: RecyclerView = root.findViewById(R.id.ratings)
        rvRatingView.layoutManager = LinearLayoutManager(context)
        rvRatingView.layoutManager!!.isAutoMeasureEnabled = true
        rvRatingView.isNestedScrollingEnabled = false
        rvRatingView.setHasFixedSize(false)
        rvRatingView.adapter = mUGCRatingAdapter
        val args = arguments ?: throw AssertionError("Args must be passed to this fragment!")
        UiUtils.showIf(
            args.getBoolean(ARG_CAN_BE_REVIEWED),
            mReviewEditText
        )
        val ratings: List<UGC.Rating>? =
            args.getParcelableArrayList(ARG_RATING_LIST)
        ratings?.let { setDefaultRatingValue(args, it) }
        return root
    }

    private fun setDefaultRatingValue(
        args: Bundle,
        ratings: List<UGC.Rating>
    ) {
        for (rating in ratings) rating.mValue =
            args.getInt(ARG_DEFAULT_RATING, 0).toFloat()
        mUGCRatingAdapter.items = ratings
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbarController.setTitle(arguments!!.getString(ARG_TITLE))
        val submitButton =
            toolbarController.toolbar.findViewById<View>(R.id.submit)
        submitButton.setOnClickListener { v: View? -> onSubmitButtonClick() }
    }

    override fun onCreateToolbarController(root: View): ToolbarController {
        return object : ToolbarController(root, activity!!) {
            override fun onUpClick() {
                Statistics.INSTANCE.trackEvent(Statistics.EventName.UGC_REVIEW_CANCEL)
                super.onUpClick()
            }
        }
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        setSaveListener(object : SaveUGCListener {
            override fun onUGCSaved(result: Boolean) {
                if (!result) {
                    finishActivity()
                    return
                }
                logUgcSaved()
                Statistics.INSTANCE.trackEvent(Statistics.EventName.UGC_REVIEW_SUCCESS)
                if (!ConnectionState.isConnected) {
                    if (isAuthorized) Utils.toastShortcut(
                        context!!,
                        R.string.ugc_thanks_message_auth
                    ) else Utils.toastShortcut(
                        context!!,
                        R.string.ugc_thanks_message_not_auth
                    )
                    finishActivity()
                    return
                }
                authorize()
            }
        })
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        setSaveListener(null)
    }

    override fun onAuthorizationFinish(success: Boolean) {
        if (success) {
            val notifier =
                from(activity!!.application)
            notifier.cancelNotification(Notifier.ID_IS_NOT_AUTHENTICATED)
            Utils.toastShortcut(context!!, R.string.ugc_thanks_message_auth)
        } else {
            Utils.toastShortcut(
                context!!,
                R.string.ugc_thanks_message_not_auth
            )
        }
        finishActivity()
    }

    private fun finishActivity() {
        if (isAdded) activity!!.finish()
    }

    override fun onAuthorizationStart() {
        finishActivity()
    }

    override fun onSocialAuthenticationCancel(@AuthTokenType type: Int) {
        Statistics.INSTANCE.trackEvent(Statistics.EventName.UGC_AUTH_DECLINED)
        Utils.toastShortcut(context!!, R.string.ugc_thanks_message_not_auth)
        finishActivity()
    }

    override fun onSocialAuthenticationError(type: Int, error: String?) {
        Statistics.INSTANCE.trackUGCAuthFailed(type, error)
        Utils.toastShortcut(context!!, R.string.ugc_thanks_message_not_auth)
        finishActivity()
    }

    private fun onSubmitButtonClick() {
        val modifiedRatings =
            mUGCRatingAdapter.items
        val update = UGCUpdate(
            modifiedRatings.toTypedArray(), mReviewEditText.text.toString(),
            System.currentTimeMillis(), Language.defaultLocale,
            Language.keyboardLocale
        )
        val featureId: FeatureId =
            arguments!!.getParcelable(ARG_FEATURE_ID)
                ?: throw AssertionError(
                    "Feature ID must be non-null for ugc object! " +
                            "Title = " + arguments!!.getString(ARG_TITLE) +
                            "; address = " + arguments!!.getString(ARG_ADDRESS) +
                            "; lat = " + arguments!!.getDouble(ARG_LAT) +
                            "; lon = " + arguments!!.getDouble(ARG_LON)
                )
        setUGCUpdate(featureId, update)
    }

    companion object {
        const val ARG_FEATURE_ID = "arg_feature_id"
        const val ARG_TITLE = "arg_title"
        const val ARG_DEFAULT_RATING = "arg_default_rating"
        const val ARG_RATING_LIST = "arg_rating_list"
        const val ARG_CAN_BE_REVIEWED = "arg_can_be_reviewed"
        const val ARG_LAT = "arg_lat"
        const val ARG_LON = "arg_lon"
        const val ARG_ADDRESS = "arg_address"
    }
}