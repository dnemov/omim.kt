package com.mapswithme.maps.bookmarks

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.text.TextUtils
import androidx.core.app.JobIntentService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.bookmarks.data.Error
import com.mapswithme.maps.bookmarks.data.Result
import com.mapswithme.maps.purchase.BookmarkPaymentDataParser
import com.mapswithme.maps.purchase.PaymentDataParser
import com.mapswithme.util.Utils
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

class SystemDownloadCompletedService : JobIntentService() {
    override fun onCreate() {
        super.onCreate()
        val app = application as MwmApplication
        if (app.arePlatformAndCoreInitialized()) return
        app.initCore()
    }

    override fun onHandleWork(intent: Intent) {
        val manager =
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                ?: throw IllegalStateException("Failed to get a download manager")
        val status = calculateStatus(manager, intent)
        val logger =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        val tag = SystemDownloadCompletedService::class.java.simpleName
        logger.i(tag, "Download status: $status")
        UiThread.run(
            SendStatusTask(
                applicationContext,
                status
            )
        )
    }

    private fun calculateStatus(manager: DownloadManager, intent: Intent): OperationStatus {
        return try {
            calculateStatusInternal(manager, intent)
        } catch (e: Exception) {
            OperationStatus(null, Error(e.message))
        }
    }

    @Throws(IOException::class)
    private fun calculateStatusInternal(
        manager: DownloadManager, intent: Intent
    ): OperationStatus {
        var cursor: Cursor? = null
        try {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
            val query = DownloadManager.Query().setFilterById(id)
            cursor = manager.query(query)
            if (cursor.moveToFirst()) {
                if (isDownloadFailed(cursor)) {
                    val error =
                        Error(
                            getHttpStatus(cursor),
                            getErrorMessage(cursor)
                        )
                    return OperationStatus(null, error)
                }
                logToPushWoosh(
                    applicationContext as Application,
                    cursor
                )
                val result =
                    Result(
                        getFilePath(cursor),
                        getArchiveId(cursor)
                    )
                return OperationStatus(result, null)
            }
            throw IOException("Failed to move the cursor at first row")
        } finally {
            Utils.closeSafely(cursor!!)
        }
    }

    private class SendStatusTask(
        private val mAppContext: Context,
        private val mStatus: OperationStatus
    ) : Runnable {
        override fun run() {
            val intent =
                Intent(ACTION_DOWNLOAD_COMPLETED)
            intent.putExtra(EXTRA_DOWNLOAD_STATUS, mStatus)
            LocalBroadcastManager.getInstance(mAppContext)
                .sendBroadcast(intent)
        }

    }

    companion object {
        const val ACTION_DOWNLOAD_COMPLETED = "action_download_completed"
        const val EXTRA_DOWNLOAD_STATUS = "extra_download_status"
        private fun isDownloadFailed(cursor: Cursor): Boolean {
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            return status != DownloadManager.STATUS_SUCCESSFUL
        }

        private fun getFilePath(cursor: Cursor): String? {
            val localUri = getColumnValue(
                cursor,
                DownloadManager.COLUMN_LOCAL_URI
            )
            return if (localUri == null) null else Uri.parse(localUri).path
        }

        private fun getArchiveId(cursor: Cursor): String? {
            return Uri.parse(
                getColumnValue(
                    cursor,
                    DownloadManager.COLUMN_URI
                )
            ).lastPathSegment
        }

        private fun getColumnValue(
            cursor: Cursor,
            columnName: String
        ): String? {
            return cursor.getString(cursor.getColumnIndex(columnName))
        }

        private fun getHttpStatus(cursor: Cursor): Int {
            val rawStatus =
                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            return rawStatus.toInt()
        }

        private fun getErrorMessage(cursor: Cursor): String? {
            return cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
        }

        private fun logToPushWoosh(
            application: Application,
            cursor: Cursor
        ) {
            val url = getColumnValue(
                cursor,
                DownloadManager.COLUMN_URI
            )
            if (TextUtils.isEmpty(url)) return
            val decodedUrl: String?
            decodedUrl = try {
                URLDecoder.decode(url, "UTF-8")
            } catch (exception: UnsupportedEncodingException) {
                ""
            }
            val p: PaymentDataParser = BookmarkPaymentDataParser()
            val productId =
                p.getParameterByName(decodedUrl!!, BookmarkPaymentDataParser.PRODUCT_ID)
            val name =
                p.getParameterByName(decodedUrl, BookmarkPaymentDataParser.NAME)
            val app = application as MwmApplication
            if (TextUtils.isEmpty(productId)) {
                app.sendPushWooshTags("Bookmarks_Guides_free_title", arrayOf(name!!))
            } else {
            }
        }
    }
}