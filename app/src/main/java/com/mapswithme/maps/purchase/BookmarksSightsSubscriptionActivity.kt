package com.mapswithme.maps.purchase

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragmentActivity

class BookmarksSightsSubscriptionActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = BookmarksSightsSubscriptionFragment::class.java

    override fun getThemeResourceId(theme: String): Int {
        return R.style.MwmTheme
    }

    companion object {
        fun startForResult(activity: FragmentActivity) {
            val intent = Intent(activity, BookmarksSightsSubscriptionActivity::class.java)
            activity.startActivityForResult(intent, PurchaseUtils.REQ_CODE_PAY_SUBSCRIPTION)
        }

        fun startForResult(
            fragment: Fragment, requestCode: Int,
            from: String
        ) {
            val intent =
                Intent(fragment.activity, BookmarksSightsSubscriptionActivity::class.java)
            intent.putExtra(AbstractBookmarkSubscriptionFragment.EXTRA_FROM, from).flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}