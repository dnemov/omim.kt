package com.mapswithme.maps.ads

import android.util.SparseArray
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.downloader.DownloaderFragment
import com.mapswithme.maps.downloader.MapManager
import com.mapswithme.maps.editor.EditorHostFragment
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.maps.search.SearchFragment
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Counters
import com.mapswithme.util.concurrency.UiThread
import java.lang.ref.WeakReference
import java.util.*

enum class LikesManager {
    INSTANCE;

    /*
   Maps type of like dialog to the dialog, performing like.
  */
    enum class LikeType(
        val clazz: Class<out DialogFragment>,
        val delay: Int
    ) {
        GPLAY_NEW_USERS(
            RateStoreDialogFragment::class.java,
            DIALOG_DELAY_DEFAULT
        ),
        GPLAY_OLD_USERS(
            RateStoreDialogFragment::class.java, DIALOG_DELAY_DEFAULT
        ),
        FACEBOOK_INVITE_NEW_USERS(
            FacebookInvitesDialogFragment::class.java, DIALOG_DELAY_DEFAULT
        ),
        FACEBOOK_INVITES_OLD_USERS(
            FacebookInvitesDialogFragment::class.java, DIALOG_DELAY_DEFAULT
        );

    }

    companion object {
        private const val DIALOG_DELAY_DEFAULT = 30000
        private const val DIALOG_DELAY_SHORT = 5000
        private val SESSION_NUM = Counters.getSessionCount()
        /*
   Maps number of session to LikeType.
  */
        private val sOldUsersMapping = SparseArray<LikeType>()
        private val sNewUsersMapping = SparseArray<LikeType>()
        private val sFragments: MutableList<Class<out Fragment?>> =
            ArrayList()

        init {
            sOldUsersMapping.put(6, LikeType.FACEBOOK_INVITES_OLD_USERS)
            sOldUsersMapping.put(30, LikeType.FACEBOOK_INVITES_OLD_USERS)
            sOldUsersMapping.put(50, LikeType.FACEBOOK_INVITES_OLD_USERS)
            sNewUsersMapping.put(9, LikeType.FACEBOOK_INVITE_NEW_USERS)
            sNewUsersMapping.put(35, LikeType.FACEBOOK_INVITE_NEW_USERS)
            sNewUsersMapping.put(55, LikeType.FACEBOOK_INVITE_NEW_USERS)
            sFragments.add(SearchFragment::class.java)
            sFragments.add(EditorHostFragment::class.java)
            sFragments.add(DownloaderFragment::class.java)
        }
    }

    val isNewUser =
        Counters.getFirstInstallVersion() == BuildConfig.VERSION_CODE
    private var mLikeRunnable: Runnable? = null
    private var mActivityRef: WeakReference<FragmentActivity>? = null

    fun showDialogs(activity: FragmentActivity) {
        mActivityRef = WeakReference(activity)
        if (!ConnectionState.isConnected) return
        val type =
            if (isNewUser) sNewUsersMapping[SESSION_NUM] else sOldUsersMapping[SESSION_NUM]
        if (type != null) displayLikeDialog(type.clazz, type.delay)
    }

    fun showRateDialogForOldUser(activity: FragmentActivity) {
        if (isNewUser) return
        mActivityRef = WeakReference(activity)
        displayLikeDialog(LikeType.GPLAY_OLD_USERS.clazz, LikeType.GPLAY_OLD_USERS.delay)
    }

    fun cancelDialogs() {
        UiThread.cancelDelayedTasks(mLikeRunnable)
    }

    private fun containsFragments(activity: MwmActivity): Boolean {
        for (fragmentClass in sFragments) {
            if (activity.containsFragment(fragmentClass)) return true
        }
        return false
    }

    private fun displayLikeDialog(
        dialogFragmentClass: Class<out DialogFragment>,
        delayMillis: Int
    ) {
        if (Counters.isSessionRated(SESSION_NUM) || Counters.isRatingApplied(
                dialogFragmentClass
            )
        ) return
        Counters.setRatedSession(SESSION_NUM)
        UiThread.cancelDelayedTasks(mLikeRunnable)
        mLikeRunnable = Runnable {
            val activity = mActivityRef!!.get()
            if (activity == null || activity.isFinishing || RoutingController.get().isNavigating
                || RoutingController.get().isPlanning || MapManager.nativeIsDownloading()
            ) {
                return@Runnable
            }
            if (activity !is MwmActivity) return@Runnable
            val mwmActivity = activity
            if (!mwmActivity.isMapAttached || containsFragments(mwmActivity)) return@Runnable
            val fragment: DialogFragment
            try {
                fragment = dialogFragmentClass.newInstance()
                fragment.show(activity.getSupportFragmentManager(), null)
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        UiThread.runLater(mLikeRunnable, delayMillis.toLong())
    }
}