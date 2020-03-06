package com.mapswithme.maps.settings

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseToolbarActivity

class SettingsActivity : BaseToolbarActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private var mLastTitle: String? = null
    override val contentLayoutResId: Int
        protected get() = R.layout.activity_settings

    override val fragmentClass: Class<out Fragment>
        protected get() = SettingsPrefsFragment::class.java

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val title =
            if (TextUtils.isEmpty(pref.title)) null else pref.title.toString()
        try {
            val fragment =
                Class.forName(pref.fragment) as Class<out Fragment>
            replaceFragment(fragment, title, pref.extras)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return true
    }

    override fun onPreferenceStartScreen(
        preferenceFragmentCompat: PreferenceFragmentCompat,
        preferenceScreen: PreferenceScreen
    ): Boolean {
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.key)
        replaceFragment(
            SettingsPrefsFragment::class.java,
            preferenceScreen.title.toString(),
            args
        )
        return true
    }

    fun replaceFragment(
        fragmentClass: Class<out Fragment>,
        title: String?, args: Bundle?
    ) {
        val resId = fragmentContentResId
        check(!(resId <= 0 || findViewById<View?>(resId) == null)) {
            "Fragment can't be added, since getFragmentContentResId() " +
                    "isn't implemented or returns wrong resourceId."
        }
        val name = fragmentClass.name
        val fragment =
            Fragment.instantiate(this, name, args)
        supportFragmentManager.beginTransaction()
            .replace(resId, fragment, name)
            .addToBackStack(null)
            .commitAllowingStateLoss()
        supportFragmentManager.executePendingTransactions()
        if (title != null) {
            val toolbar = toolbar
            if (toolbar != null && toolbar.title != null) {
                mLastTitle = toolbar.title.toString()
                toolbar.title = title
            }
        }
    }

    override fun onBackPressed() {
        if (mLastTitle != null) toolbar!!.title = mLastTitle
        super.onBackPressed()
    }
}