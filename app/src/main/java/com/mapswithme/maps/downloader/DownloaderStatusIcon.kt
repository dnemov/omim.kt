package com.mapswithme.maps.downloader

import android.util.SparseIntArray
import android.view.View
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.WheelProgressView
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils

open class DownloaderStatusIcon(private val mFrame: View) {
    protected val mIcon: ImageView
    private val mProgress: WheelProgressView
    fun setOnIconClickListener(listener: View.OnClickListener?): DownloaderStatusIcon {
        mIcon.setOnClickListener(listener)
        return this
    }

    fun setOnCancelClickListener(listener: View.OnClickListener?): DownloaderStatusIcon {
        mProgress.setOnClickListener(listener)
        return this
    }

    @AttrRes
    protected open fun selectIcon(country: CountryItem?): Int {
        return when (country!!.status) {
            CountryItem.Companion.STATUS_DONE -> R.attr.status_done
            CountryItem.Companion.STATUS_DOWNLOADABLE, CountryItem.Companion.STATUS_PARTLY -> R.attr.status_downloadable
            CountryItem.Companion.STATUS_FAILED -> R.attr.status_failed
            CountryItem.Companion.STATUS_UPDATABLE -> R.attr.status_updatable
            else -> throw IllegalArgumentException("Inappropriate item status: " + country.status)
        }
    }

    @DrawableRes
    private fun resolveIcon(@AttrRes iconAttr: Int): Int {
        var res = sIconsCache[iconAttr]
        if (res == 0) {
            res = ThemeUtils.getResource(
                mFrame.context,
                R.attr.downloaderTheme,
                iconAttr
            )
            sIconsCache.put(iconAttr, res)
        }
        return res
    }

    protected open fun updateIcon(country: CountryItem?) {
        @AttrRes val iconAttr = selectIcon(country)
        @DrawableRes val icon = resolveIcon(iconAttr)
        mIcon.setImageResource(icon)
    }

    fun update(country: CountryItem?) {
        val pending = country!!.status == CountryItem.Companion.STATUS_ENQUEUED
        val inProgress =
            country.status == CountryItem.Companion.STATUS_PROGRESS || country.status == CountryItem.Companion.STATUS_APPLYING || pending
        UiUtils.showIf(inProgress, mProgress)
        UiUtils.showIf(!inProgress, mIcon)
        mProgress.isPending = pending
        if (inProgress) {
            if (!pending) mProgress.progress = country.progress
            return
        }
        updateIcon(country)
    }

    fun show(show: Boolean) {
        UiUtils.showIf(show, mFrame)
    }

    companion object {
        private val sIconsCache = SparseIntArray()
        @kotlin.jvm.JvmStatic
        fun clearCache() {
            sIconsCache.clear()
        }
    }

    init {
        mIcon =
            mFrame.findViewById<View>(R.id.downloader_status) as ImageView
        mProgress =
            mFrame.findViewById<View>(R.id.downloader_progress_wheel) as WheelProgressView
    }
}