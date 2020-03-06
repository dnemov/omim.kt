package com.mapswithme.maps.onboarding

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.R

import com.mapswithme.maps.downloader.UpdaterDialogFragment
import com.mapswithme.util.Counters
import com.mapswithme.util.SharedPropertiesUtils

class NewsFragment : BaseNewsFragment() {
    private inner class Adapter :
        BaseNewsFragment.Adapter() {
        override val titleKeys: Int
            get() = BaseNewsFragment.Companion.TITLE_KEYS

        override val subtitles1: Int
            get() = R.array.news_messages_1

        override val subtitles2: Int
            get() = R.array.news_messages_2

        override val buttonLabels: Int
            get() = R.array.news_button_labels

        override val buttonLinks: Int
            get() = R.array.news_button_links

        override val switchTitles: Int
            get() = R.array.news_switch_titles

        override val switchSubtitles: Int
            get() = R.array.news_switch_subtitles

        override val images: Int
            get() = R.array.news_images
    }

    public override fun createAdapter(): BaseNewsFragment.Adapter {
        return Adapter()
    }

    override fun onDoneClick() {
        if (!UpdaterDialogFragment.showOn(
                activity!!,
                listener
            )
        ) super.onDoneClick() else dismissAllowingStateLoss()
    }

    companion object {
        /**
         * Displays "What's new" dialog on given `activity`. Or not.
         * @return whether "What's new" dialog should be shown.
         */
        @kotlin.jvm.JvmStatic
        fun showOn(
            activity: FragmentActivity,
            listener: NewsDialogListener?
        ): Boolean {
            if (Counters.getFirstInstallVersion() >= BuildConfig.VERSION_CODE) return false
            val fm = activity.supportFragmentManager
            if (fm.isDestroyed) return false
            val f =
                fm.findFragmentByTag(UpdaterDialogFragment::class.java.name)
            if (f != null) return UpdaterDialogFragment.showOn(activity, listener)
            val currentTitle =
                getCurrentTitleConcatenation(activity.applicationContext)
            val oldTitle = SharedPropertiesUtils.whatsNewTitleConcatenation
            if (currentTitle == oldTitle && !BaseNewsFragment.Companion.recreate(
                    activity,
                    NewsFragment::class.java
                )
            ) return false
            BaseNewsFragment.Companion.create(activity, NewsFragment::class.java, listener)
            Counters.setWhatsNewShown()
            SharedPropertiesUtils.setWhatsNewTitleConcatenation(currentTitle)
            Counters.setShowReviewForOldUser(true)
            return true
        }

        private fun getCurrentTitleConcatenation(context: Context): String {
            val keys =
                context.resources.getStringArray(BaseNewsFragment.Companion.TITLE_KEYS)
            val length = keys.size
            if (length == 0) return ""
            val sb = StringBuilder("")
            for (key in keys) sb.append(key)
            return sb.toString().trim { it <= ' ' }
        }
    }
}