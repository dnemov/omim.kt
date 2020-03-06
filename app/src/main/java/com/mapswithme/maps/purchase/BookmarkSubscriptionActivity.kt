package com.mapswithme.maps.purchase

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.util.statistics.Statistics

class BookmarkSubscriptionActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = BookmarkSubscriptionFragment::class.java

    override fun useTransparentStatusBar(): Boolean {
        return false
    }

    companion object {
        fun startForResult(activity: FragmentActivity) {
            val intent = Intent(activity, BookmarkSubscriptionActivity::class.java)
                .putExtra(
                    AbstractBookmarkSubscriptionFragment.EXTRA_FROM,
                    Statistics.ParamValue.SPONSORED_BUTTON
                )
            activity.startActivityForResult(intent, PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION)
        }

        fun startForResult(
            fragment: Fragment, requestCode: Int,
            from: String
        ) {
            val intent =
                Intent(fragment.activity, BookmarkSubscriptionActivity::class.java)
            intent.putExtra(AbstractBookmarkSubscriptionFragment.EXTRA_FROM, from).flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}