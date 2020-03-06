package com.mapswithme.maps.ads

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.ads.RateStoreDialogFragment
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.util.*
import com.mapswithme.util.UiUtils.SimpleAnimatorListener
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.Statistics

class RateStoreDialogFragment : BaseMwmDialogFragment(),
    View.OnClickListener {
    private var mRating = 0f
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder =
            AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val root = inflater.inflate(R.layout.fragment_google_play_dialog, null)
        builder.setView(root).setNegativeButton(
            getString(R.string.remind_me_later)
        ) { dialog, which -> Statistics.INSTANCE.trackEvent(Statistics.EventName.RATE_DIALOG_LATER) }
        val rateBar =
            root.findViewById<View>(R.id.rb__play_rate) as RatingBar
        rateBar.onRatingBarChangeListener =
            OnRatingBarChangeListener { ratingBar, rating, fromUser ->
                Statistics.INSTANCE.trackRatingDialog(rating)
                mRating = rating
                if (rating >= BuildConfig.RATING_THRESHOLD) {
                    Counters.setRatingApplied(RateStoreDialogFragment::class.java)
                    dismiss()
                    Utils.openAppInMarket(
                        activity,
                        BuildConfig.REVIEW_URL
                    )
                } else {
                    val animator =
                        ObjectAnimator.ofFloat(rateBar, "alpha", 1.0f, 0.0f)
                    animator.addListener(object : SimpleAnimatorListener() {
                        override fun onAnimationEnd(animation: Animator) {
                            val button =
                                root.findViewById<View>(R.id.btn__explain_bad_rating) as Button
                            UiUtils.show(button)
                            Graphics.tint(button)
                            button.setOnClickListener(this@RateStoreDialogFragment)
                            (root.findViewById<View>(R.id.tv__title) as TextView).text =
                                getString(R.string.rating_thanks)
                            (root.findViewById<View>(R.id.tv__subtitle) as TextView).text =
                                getString(R.string.rating_share_ideas)
                            root.findViewById<View>(R.id.v__divider).visibility = View.VISIBLE
                            rateBar.visibility = View.GONE
                            super.onAnimationEnd(animation)
                        }
                    })
                    animator.start()
                }
            }
        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Statistics.INSTANCE.trackEvent(Statistics.EventName.RATE_DIALOG_LATER)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn__explain_bad_rating -> {
                dismiss()
                val intent = Intent(Intent.ACTION_SENDTO)
                val info: PackageInfo
                var installTime: Long = 0
                try {
                    info = MwmApplication.get().packageManager
                        .getPackageInfo(BuildConfig.APPLICATION_ID, 0)
                    installTime = info.firstInstallTime
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
                intent.data = Utils.buildMailUri(
                    Constants.Email.RATING,
                    getString(R.string.rating_just_rated) + ": " + mRating,
                    "OS : " + Build.VERSION.SDK_INT + "\n" + "Version : " + BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME + "\n" +
                            getString(
                                R.string.rating_user_since,
                                DateUtils.formatDateTime(
                                    activity,
                                    installTime,
                                    0
                                )
                            ) + "\n\n"
                )
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    AlohaHelper.logException(e)
                }
            }
        }
    }
}