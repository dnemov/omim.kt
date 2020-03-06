package com.mapswithme.maps.bookmarks

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Pair
import com.mapswithme.maps.Framework
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.util.HttpClient
import com.mapswithme.util.log.LoggerFactory
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URLEncoder

class BookmarksDownloadManager private constructor(context: Context) {
    private val mContext: Context
    @Throws(MalformedURLException::class)
    fun enqueueRequest(url: String): Long {
        val uriPair =
            prepareUriPair(url)
        val downloadManager =
            mContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                ?: throw NullPointerException(
                    "Download manager is null, failed to download url = $url"
                )
        val srcUri = uriPair.first
        val dstUri = uriPair.second
        LOGGER.d(
            TAG,
            "Bookmarks catalog url = $dstUri"
        )
        val request = DownloadManager.Request(dstUri)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .addRequestHeader(
                HttpClient.HEADER_USER_AGENT,
                Framework.nativeGetUserAgent()
            )
            .addRequestHeader(
                HttpClient.HEADER_DEVICE_ID,
                Framework.nativeGetDeviceId()
            )
            .setDestinationInExternalFilesDir(mContext, null, dstUri.lastPathSegment)
        val accessToken = Framework.nativeGetAccessToken()
        if (!TextUtils.isEmpty(accessToken)) {
            LOGGER.d(
                TAG,
                "User authorized"
            )
            val headerValue =
                HttpClient.HEADER_BEARER_PREFFIX + accessToken
            request.addRequestHeader(
                HttpClient.HEADER_AUTHORIZATION,
                headerValue
            )
        } else {
            LOGGER.d(
                TAG,
                "User not authorized"
            )
        }
        val title = makeTitle(srcUri)
        if (!TextUtils.isEmpty(title)) request.setTitle(title)
        return downloadManager.enqueue(request)
    }

    companion object {
        private const val QUERY_PARAM_ID_KEY = "id"
        private const val QUERY_PARAM_NAME_KEY = "name"
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = BookmarksDownloadManager::class.java.simpleName
        private fun makeTitle(srcUri: Uri): String? {
            val title =
                srcUri.getQueryParameter(QUERY_PARAM_NAME_KEY)
            return if (TextUtils.isEmpty(title)) srcUri.getQueryParameter(QUERY_PARAM_ID_KEY) else title
        }

        @Throws(MalformedURLException::class)
        private fun prepareUriPair(url: String): Pair<Uri, Uri> {
            val srcUri = Uri.parse(url)
            val fileId =
                srcUri.getQueryParameter(QUERY_PARAM_ID_KEY)
            if (TextUtils.isEmpty(fileId)) throw MalformedURLException("File id not found")
            val downloadUrl = BookmarkManager.INSTANCE.getCatalogDownloadUrl(fileId!!)
            val builder = Uri.parse(downloadUrl).buildUpon()
            for (each in srcUri.queryParameterNames) {
                var queryParameter = srcUri.getQueryParameter(each)
                queryParameter = try {
                    URLEncoder.encode(queryParameter, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    srcUri.getQueryParameter(each)
                }
                builder.appendQueryParameter(each, queryParameter)
            }
            val dstUri = builder.build()
            return Pair(srcUri, dstUri)
        }

        @JvmStatic
        fun from(context: Context): BookmarksDownloadManager {
            return BookmarksDownloadManager(context)
        }
    }

    init {
        mContext = context.applicationContext
    }
}