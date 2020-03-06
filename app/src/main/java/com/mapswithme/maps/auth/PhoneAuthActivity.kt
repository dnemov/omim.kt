package com.mapswithme.maps.auth

import android.content.Intent
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmExtraTitleActivity
import com.mapswithme.maps.base.OnBackPressListener

class PhoneAuthActivity : BaseMwmExtraTitleActivity() {
    override fun onBackPressed() {
        val manager = supportFragmentManager
        val fragment = manager.findFragmentByTag(
            PhoneAuthFragment::class.java.name
        )
            ?: return
        if (!(fragment as OnBackPressListener).onBackPressed()) super.onBackPressed()
    }

    override val fragmentClass: Class<out Fragment>?
        get() = PhoneAuthFragment::class.java

    companion object {
        fun startForResult(fragment: Fragment) {
            val i = Intent(fragment.context, PhoneAuthActivity::class.java)
            i.putExtra(
                EXTRA_TITLE,
                fragment.getString(R.string.authorization_button_sign_in)
            )
            fragment.startActivityForResult(
                i,
                Constants.REQ_CODE_PHONE_AUTH_RESULT
            )
        }
    }
}