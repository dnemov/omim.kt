package com.mapswithme.maps.purchase

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.bookmarks.data.PaymentData

class BookmarkPaymentActivity : BaseMwmFragmentActivity() {
    override fun onSafeCreate(savedInstanceState: Bundle?) {
        super.onSafeCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }

    override val fragmentClass: Class<out Fragment>
        protected get() = BookmarkPaymentFragment::class.java

    companion object {
        fun startForResult(
            fragment: Fragment, paymentData: PaymentData,
            requestCode: Int
        ) {
            val intent = Intent(fragment.activity, BookmarkPaymentActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            val args = Bundle()
            args.putParcelable(BookmarkPaymentFragment.Companion.ARG_PAYMENT_DATA, paymentData)
            intent.putExtras(args)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}