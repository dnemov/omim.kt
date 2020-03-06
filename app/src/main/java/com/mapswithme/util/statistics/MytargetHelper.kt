package com.mapswithme.util.statistics

import android.app.Activity
import androidx.annotation.WorkerThread
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.util.ConnectionState.isConnected
import com.mapswithme.util.concurrency.ThreadPool
import com.mapswithme.util.concurrency.UiThread
import com.my.target.nativeads.NativeAppwallAd
import com.my.target.nativeads.banners.NativeAppwallBanner
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MytargetHelper(listener: Listener<Void?>) {
    private var mShowcase: NativeAppwallAd? = null
    private var mCancelled = false

    interface Listener<T> {
        fun onNoAds()
        fun onDataReady(data: T?)
    }

    fun cancel() {
        mCancelled = true
    }

    fun loadShowcase(
        listener: Listener<List<NativeAppwallBanner>>,
        activity: Activity
    ) {
        if (mShowcase == null) mShowcase = loadAds(listener, activity)
    }

    fun handleBannersShow(banners: List<NativeAppwallBanner>) {
        if (mShowcase != null) mShowcase!!.handleBannersShow(banners)
    }

    private fun loadAds(
        listener: Listener<List<NativeAppwallBanner>>,
        activity: Activity
    ): NativeAppwallAd {
        val res = NativeAppwallAd(PrivateVariables.myTargetSlot(), activity)
        res.setListener(object : NativeAppwallAd.AppwallAdListener {


            override fun onLoad(p0: NativeAppwallAd) {
                if (mCancelled) return
                if (p0.getBanners().isEmpty()) listener.onNoAds() else listener.onDataReady(p0.getBanners())
            }

            override fun onClick(p0: NativeAppwallBanner, p1: NativeAppwallAd) {

            }

            override fun onDisplay(p0: NativeAppwallAd) {

            }

            override fun onDismiss(p0: NativeAppwallAd) {

            }


            override fun onNoAd(p0: String, p1: NativeAppwallAd) {
                listener.onNoAds()
            }
        })
        res.load()
        return res
    }

    fun onBannerClick(banner: NativeAppwallBanner) {
        if (mShowcase != null) mShowcase!!.handleBannerClick(banner)
    }

    companion object {
        // for caching of myTarget setting achieved from server
        private const val PREF_CHECK = "MyTargetCheck"
        private const val PREF_CHECK_MILLIS = "MyTargetCheckTimestamp"
        private val CHECK_URL: String = PrivateVariables.myTargetCheckUrl()
        private val CHECK_INTERVAL_MILLIS: Long =
            PrivateVariables.myTargetCheckInterval() * 1000
        private const val TIMEOUT = 1000
        // bugfix for HEAD requests on pre-JB devices https://code.google.com/p/android/issues/detail?id=24672
        @get:WorkerThread
        private val showcaseSetting: Boolean
            private get() {
                val lastCheckMillis: Long = MwmApplication.prefs()?.getLong(
                    PREF_CHECK_MILLIS,
                    0
                ) ?: 0
                val currentMillis = System.currentTimeMillis()
                if (currentMillis - lastCheckMillis < CHECK_INTERVAL_MILLIS) return isShowcaseSwitchedOnServer
                var connection: HttpURLConnection? = null
                try {
                    val url =
                        URL(CHECK_URL)
                    connection = url.openConnection() as HttpURLConnection
                    connection!!.requestMethod = "HEAD"
                    // bugfix for HEAD requests on pre-JB devices https://code.google.com/p/android/issues/detail?id=24672
                    connection.setRequestProperty("Accept-Encoding", "")
                    connection.connectTimeout = TIMEOUT
                    connection.readTimeout = TIMEOUT
                    connection.connect()
                    val showShowcase =
                        connection.responseCode == HttpURLConnection.HTTP_OK
                    isShowcaseSwitchedOnServer = showShowcase
                    return showShowcase
                } catch (ignored: MalformedURLException) {
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    connection?.disconnect()
                }
                return false
            }

        var isShowcaseSwitchedOnServer: Boolean
            get() = MwmApplication.prefs()?.getBoolean(
                PREF_CHECK,
                true
            ) ?: true
            private set(switchedOn) {
                MwmApplication.prefs()?.edit()
                    ?.putLong(
                        PREF_CHECK_MILLIS,
                        System.currentTimeMillis()
                    )
                    ?.putBoolean(
                        PREF_CHECK,
                        switchedOn
                    )
                    ?.apply()
            }

    }

    init {
        if (!isConnected) {
            listener.onNoAds()
        }
        ThreadPool.worker.execute(Runnable {
            val showShowcase =
                showcaseSetting
            if (mCancelled) return@Runnable
            UiThread.run(Runnable {
                if (mCancelled) return@Runnable
                if (showShowcase) listener.onDataReady(null) else listener.onNoAds()
            })
        })
    }
}