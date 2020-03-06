package com.mapswithme.util

import android.app.Fragment
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import java.lang.ref.WeakReference
import java.util.*

/**
 * Helper class to track fragments attached to Activity.
 * Its primary goal is to implement getFragments() that is present in Support Library
 * but is missed in native FragmentManager.
 *
 *
 *
 * Usage:
 *
 *  * Create instance of FragmentListHelper in your Activity.
 *  * Override [android.app.Activity.onAttachFragment] in your Activity and call [FragmentListHelper.onAttachFragment].
 *  * Call [FragmentListHelper.getFragments] to obtain list of fragments currently added to your Activity.
 *
 */
class FragmentListHelper {
    private val mFragments: MutableMap<String, WeakReference<Fragment>> =
        HashMap()

    fun onAttachFragment(fragment: Fragment) {
        mFragments[fragment.javaClass.name] = WeakReference(fragment)
    }

    val fragments: List<Fragment>
        get() {
            var toRemove: MutableList<String>? = null
            val res: MutableList<Fragment> =
                ArrayList(mFragments.size)
            for (key in mFragments.keys) {
                val f = mFragments[key]!!.get()
                if (f == null || !f.isAdded) {
                    if (toRemove == null) toRemove = ArrayList()
                    toRemove.add(key)
                    continue
                }
                res.add(f)
            }
            if (toRemove != null) for (key in toRemove) mFragments.remove(key)
            return res
        }
}