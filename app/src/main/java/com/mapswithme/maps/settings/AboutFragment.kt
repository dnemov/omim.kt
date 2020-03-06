package com.mapswithme.maps.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.util.Constants
import com.mapswithme.util.Graphics
import com.mapswithme.util.Utils
import com.mapswithme.util.sharing.ShareOption
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.Statistics

class AboutFragment : BaseSettingsFragment(), View.OnClickListener {
    private fun setupItem(
        @IdRes id: Int, tint: Boolean,
        frame: View
    ) {
        val view = frame.findViewById<TextView>(id)
        view.setOnClickListener(this)
        if (tint) Graphics.tint(view)
    }

    protected override val layoutRes: Int
        protected get() = R.layout.about

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        (root!!.findViewById<View>(R.id.version) as TextView).text =
            getString(R.string.version, BuildConfig.VERSION_NAME)
        (root.findViewById<View>(R.id.data_version) as TextView).text =
            getString(R.string.data_version, Framework.nativeGetDataVersion())
        setupItem(R.id.web, true, root)
        setupItem(R.id.facebook, false, root)
        setupItem(R.id.twitter, false, root)
        setupItem(R.id.rate, true, root)
        setupItem(R.id.share, true, root)
        setupItem(R.id.copyright, false, root)
        val termOfUseView =
            root.findViewById<View>(R.id.term_of_use_link)
        val privacyPolicyView =
            root.findViewById<View>(R.id.privacy_policy)
        termOfUseView.setOnClickListener { v: View? -> onTermOfUseClick() }
        privacyPolicyView.setOnClickListener { v: View? -> onPrivacyPolicyClick() }
        return root
    }

    private fun openLink(link: String) {
        Utils.openUrl(activity!!, link)
    }

    private fun onPrivacyPolicyClick() {
        openLink(Framework.nativeGetPrivacyPolicyLink())
    }

    private fun onTermOfUseClick() {
        openLink(Framework.nativeGetTermsOfUseLink())
    }

    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.web -> {
                    Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.WEB_SITE)
                    AlohaHelper.logClick(AlohaHelper.Settings.WEB_SITE)
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(Constants.Url.WEB_SITE)
                        )
                    )
                }
                R.id.facebook -> {
                    Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.FACEBOOK)
                    AlohaHelper.logClick(AlohaHelper.Settings.FACEBOOK)
                    Utils.showFacebookPage(activity!!)
                }
                R.id.twitter -> {
                    Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.TWITTER)
                    AlohaHelper.logClick(AlohaHelper.Settings.TWITTER)
                    Utils.showTwitterPage(activity!!)
                }
                R.id.rate -> {
                    Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.RATE)
                    AlohaHelper.logClick(AlohaHelper.Settings.RATE)
                    Utils.openAppInMarket(
                        activity,
                        BuildConfig.REVIEW_URL
                    )
                }
                R.id.share -> {
                    Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.TELL_FRIEND)
                    AlohaHelper.logClick(AlohaHelper.Settings.TELL_FRIEND)
                    ShareOption.AnyShareOption.ANY.share(
                        activity, getString(R.string.tell_friends_text),
                        R.string.tell_friends
                    )
                }
                R.id.copyright -> {
                    Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.COPYRIGHT)
                    AlohaHelper.logClick(AlohaHelper.Settings.COPYRIGHT)
                    settingsActivity?.replaceFragment(
                        CopyrightFragment::class.java,
                        getString(R.string.copyright), null
                    )
                }
            }
        } catch (e: ActivityNotFoundException) {
            AlohaHelper.logException(e)
        }
    }
}