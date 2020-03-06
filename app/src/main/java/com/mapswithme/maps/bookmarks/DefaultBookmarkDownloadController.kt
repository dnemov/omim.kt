package com.mapswithme.maps.bookmarks

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import com.mapswithme.maps.bookmarks.BookmarksDownloadManager.Companion.from
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksCatalogListener
import com.mapswithme.maps.bookmarks.data.PaymentData
import com.mapswithme.maps.purchase.BookmarkPaymentDataParser
import com.mapswithme.maps.purchase.PaymentDataParser
import com.mapswithme.util.log.LoggerFactory
import java.net.MalformedURLException

internal class DefaultBookmarkDownloadController(
    private val mApplication: Application,
    private val mCatalogListener: BookmarksCatalogListener
) : BookmarkDownloadController, BookmarkDownloadHandler {
    private val mDownloadCompleteReceiver = BookmarkDownloadReceiver()
    private var mDownloadUrl: String? = null
    private var mCallback: BookmarkDownloadCallback? = null
    override fun downloadBookmark(url: String): Boolean {
        return try {
            downloadBookmarkInternal(mApplication, url)
            mDownloadUrl = url
            true
        } catch (e: MalformedURLException) {
            LOGGER.e(
                TAG,
                "Failed to download bookmark, url: $url"
            )
            false
        }
    }

    override fun retryDownloadBookmark() {
        if (TextUtils.isEmpty(mDownloadUrl)) return
        try {
            downloadBookmarkInternal(
                mApplication,
                mDownloadUrl!!
            )
        } catch (e: MalformedURLException) {
            LOGGER.e(
                TAG,
                "Failed to retry bookmark downloading, url: $mDownloadUrl"
            )
        }
    }

    override fun attach(callback: BookmarkDownloadCallback?) {
        if (mCallback != null) throw AssertionError("Already attached! Call detach.")
        mCallback = callback
        mDownloadCompleteReceiver.attach(this)
        mDownloadCompleteReceiver.register(mApplication)
        BookmarkManager.INSTANCE.addCatalogListener(mCatalogListener)
    }

    override fun detach() {
        if (mCallback == null) throw AssertionError("Already detached! Call attach.")
        mDownloadCompleteReceiver.detach()
        mDownloadCompleteReceiver.unregister(mApplication)
        BookmarkManager.INSTANCE.removeCatalogListener(mCatalogListener)
        mCallback = null
    }

    override fun onAuthorizationRequired() {
        LOGGER.i(
            TAG,
            "Authorization required for bookmark purchase"
        )
        if (mCallback != null) mCallback!!.onAuthorizationRequired()
    }

    override fun onPaymentRequired() {
        LOGGER.i(
            TAG,
            "Payment required for bookmark purchase"
        )
        check(!TextUtils.isEmpty(mDownloadUrl)) { "Download url must be non-null if payment required!" }
        if (mCallback != null) {
            val data =
                parsePaymentData(mDownloadUrl!!)
            mCallback!!.onPaymentRequired(data)
        }
    }

    override fun onSave(outState: Bundle?) {
        outState?.putString(
            EXTRA_DOWNLOAD_URL,
            mDownloadUrl
        )
    }

    override fun onRestore(inState: Bundle?) {
        mDownloadUrl =
            inState?.getString(EXTRA_DOWNLOAD_URL)
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG =
            DefaultBookmarkDownloadController::class.java.simpleName
        private const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        @Throws(MalformedURLException::class)
        private fun downloadBookmarkInternal(
            context: Context,
            url: String
        ) {
            val dm = from(context)
            dm.enqueueRequest(url)
        }

        private fun createPaymentDataParser(): PaymentDataParser {
            return BookmarkPaymentDataParser()
        }

        private fun parsePaymentData(url: String): PaymentData {
            val parser =
                createPaymentDataParser()
            return parser.parse(url)
        }
    }

}