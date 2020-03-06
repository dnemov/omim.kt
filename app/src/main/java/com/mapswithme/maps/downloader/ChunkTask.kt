package com.mapswithme.maps.downloader

import android.os.AsyncTask
import android.util.Base64
import com.mapswithme.util.Constants
import com.mapswithme.util.HttpClient
import com.mapswithme.util.StringUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.log.LoggerFactory
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class ChunkTask(
    private val mHttpCallbackID: Long,
    private val mUrl: String,
    private val chunkID: Long,
    private val mEnd: Long,
    private val mExpectedFileSize: Long,
    private var mPostBody: ByteArray?,
    private val mUserAgent: String
) : AsyncTask<Void, ByteArray, Int>() {
    private var mDownloadedBytes: Long = 0
    override fun onPreExecute() {}

    override fun onPostExecute(httpOrErrorCode: Int) { //Log.i(TAG, "Writing chunk " + getChunkID());
// It seems like onPostExecute can be called (from GUI thread queue)
// after the task was cancelled in destructor of HttpThread.
// Reproduced by Samsung testers: touch Try Again for many times from
// start activity when no connection is present.
        if (!isCancelled) nativeOnFinish(
            mHttpCallbackID,
            httpOrErrorCode.toLong(),
            chunkID,
            mEnd
        )
    }

    override fun onProgressUpdate(vararg data: ByteArray) {
        if (!isCancelled) { // Use progress event to save downloaded bytes.
            if (nativeOnWrite(
                    mHttpCallbackID,
                    chunkID + mDownloadedBytes,
                    data[0],
                    data[0].size.toLong()
                )
            ) mDownloadedBytes += data[0]
                .size.toLong() else { // Cancel downloading and notify about error.
                cancel(false)
                nativeOnFinish(
                    mHttpCallbackID,
                    WRITE_EXCEPTION.toLong(),
                    chunkID,
                    mEnd
                )
            }
        }
    }

    fun start() {
        executeOnExecutor(sExecutors)
    }

    protected override fun doInBackground(vararg p: Void): Int { //Log.i(TAG, "Start downloading chunk " + getChunkID());
        var urlConnection: HttpURLConnection? = null
        /*
     * TODO improve reliability of connections & handle EOF errors.
     * <a href="http://stackoverflow.com/questions/19258518/android-httpurlconnection-eofexception">asd</a>
     */return try {
            val url = URL(mUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            if (isCancelled) return CANCELLED
            urlConnection!!.useCaches = false
            urlConnection.connectTimeout = TIMEOUT_IN_SECONDS * 1000
            urlConnection.readTimeout = TIMEOUT_IN_SECONDS * 1000
            // Set user agent with unique client id
            urlConnection.setRequestProperty(
                HttpClient.HEADER_USER_AGENT,
                mUserAgent
            )
            // Provide authorization credentials
            val creds = url.userInfo
            if (creds != null) {
                val value = "Basic " + Base64.encodeToString(
                    creds.toByteArray(),
                    Base64.DEFAULT
                )
                urlConnection.setRequestProperty("Authorization", value)
            }
            // use Range header only if we don't download whole file from start
            if (!(chunkID == 0L && mEnd < 0)) {
                if (mEnd > 0) urlConnection.setRequestProperty(
                    "Range",
                    StringUtils.formatUsingUsLocale("bytes=%d-%d", chunkID, mEnd)
                ) else urlConnection.setRequestProperty(
                    "Range",
                    StringUtils.formatUsingUsLocale("bytes=%d-", chunkID)
                )
            }
            val requestParams: Map<*, *> = urlConnection.requestProperties
            if (mPostBody != null) {
                urlConnection.doOutput = true
                urlConnection.setFixedLengthStreamingMode(mPostBody!!.size)
                val os =
                    DataOutputStream(urlConnection.outputStream)
                os.write(mPostBody)
                os.flush()
                mPostBody = null
                Utils.closeSafely(os)
            }
            if (isCancelled) return CANCELLED
            val err = urlConnection.responseCode
            if (err == HttpURLConnection.HTTP_NOT_FOUND) return err
            // @TODO We can handle redirect (301, 302 and 307) here and display redirected page to user,
// to avoid situation when downloading is always failed by "unknown" reason
// When we didn't ask for chunks, code should be 200
// When we asked for a chunk, code should be 206
            val isChunk = !(chunkID == 0L && mEnd < 0)
            if (isChunk && err != HttpURLConnection.HTTP_PARTIAL || !isChunk && err != HttpURLConnection.HTTP_OK) { // we've set error code so client should be notified about the error
                LOGGER.w(
                    TAG, "Error for " + urlConnection.url +
                            ": Server replied with code " + err +
                            ", aborting download. " + Utils.mapPrettyPrint(
                        requestParams
                    )
                )
                return INCONSISTENT_FILE_SIZE
            }
            // Check for content size - are we downloading requested file or some router's garbage?
            if (mExpectedFileSize > 0) {
                var contentLength =
                    parseContentRange(urlConnection.getHeaderField("Content-Range"))
                if (contentLength < 0) contentLength = urlConnection.contentLength.toLong()
                // Check even if contentLength is invalid (-1), in this case it's not our server!
                if (contentLength != mExpectedFileSize) { // we've set error code so client should be notified about the error
                    LOGGER.w(
                        TAG, "Error for " + urlConnection.url +
                                ": Invalid file size received (" + contentLength + ") while expecting " + mExpectedFileSize +
                                ". Aborting download."
                    )
                    return INCONSISTENT_FILE_SIZE
                }
                // @TODO Else display received web page to user - router is redirecting us to some page
            }
            downloadFromStream(
                BufferedInputStream(
                    urlConnection.inputStream,
                    65536
                )
            )
        } catch (ex: MalformedURLException) {
            LOGGER.e(TAG, "Invalid url: $mUrl", ex)
            INVALID_URL
        } catch (ex: IOException) {
            LOGGER.d(
                TAG,
                "IOException in doInBackground for URL: $mUrl",
                ex
            )
            IO_EXCEPTION
        } finally {
            urlConnection?.disconnect()
        }
    }

    private fun downloadFromStream(stream: InputStream): Int { // Because of timeouts in InputStream.read (for bad connection),
// try to introduce dynamic buffer size to read in one query.
        val arrSize = intArrayOf(64, 32, 1)
        var ret = IO_EXCEPTION
        for (size in arrSize) {
            try {
                ret = downloadFromStreamImpl(stream, size * Constants.KB)
                break
            } catch (ex: IOException) {
                LOGGER.e(
                    TAG,
                    "IOException in downloadFromStream for chunk size: $size",
                    ex
                )
            }
        }
        Utils.closeSafely(stream)
        return ret
    }

    /**
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun downloadFromStreamImpl(stream: InputStream, bufferSize: Int): Int {
        val tempBuf = ByteArray(bufferSize)
        var readBytes: Int
        while (stream.read(tempBuf).also { readBytes = it } > 0) {
            if (isCancelled) return CANCELLED
            val chunk = ByteArray(readBytes)
            System.arraycopy(tempBuf, 0, chunk, 0, readBytes)
            publishProgress(chunk)
        }
        // -1 - means the end of the stream (success), else - some error occurred
        return if (readBytes == -1) HttpURLConnection.HTTP_OK else IO_EXCEPTION
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.DOWNLOADER)
        private const val TAG = "ChunkTask"
        private const val TIMEOUT_IN_SECONDS = 60
        private const val IO_EXCEPTION = -1
        private const val WRITE_EXCEPTION = -2
        private const val INCONSISTENT_FILE_SIZE = -3
        private const val NON_HTTP_RESPONSE = -4
        private const val INVALID_URL = -5
        private const val CANCELLED = -6
        private val sExecutors: Executor =
            Executors.newFixedThreadPool(4)

        private fun parseContentRange(contentRangeValue: String?): Long {
            if (contentRangeValue != null) {
                val slashIndex = contentRangeValue.lastIndexOf('/')
                if (slashIndex >= 0) {
                    try {
                        return contentRangeValue.substring(slashIndex + 1).toLong()
                    } catch (ex: NumberFormatException) { // Return -1 at the end of function
                    }
                }
            }
            return -1
        }

        @JvmStatic private external fun nativeOnWrite(
            httpCallbackID: Long,
            beg: Long,
            data: ByteArray,
            size: Long
        ): Boolean

        @JvmStatic private external fun nativeOnFinish(
            httpCallbackID: Long,
            httpCode: Long,
            beg: Long,
            end: Long
        )
    }

}