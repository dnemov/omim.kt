package com.mapswithme.util

import android.os.Build
import android.text.TextUtils
import android.util.Base64
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.Framework
import com.mapswithme.util.Utils.closeSafely
import com.mapswithme.util.Utils.makeUrlSafe
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class HttpUploader(
    private val mMethod: String, private val mUrl: String, params: Array<KeyValue>,
    headers: Array<KeyValue>, private val mFileKey: String, private val mFilePath: String,
    needClientAuth: Boolean
) {
    private val mParams: List<KeyValue>
    private val mHeaders: MutableList<KeyValue>
    private val mBoundary: String
    private val mEndPart: String
    private val mNeedClientAuth: Boolean
    fun upload(): Result {
        var status = STATUS_CODE_UNKNOWN
        var message: String
        var writer: PrintWriter? = null
        var reader: BufferedReader? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(mUrl)
            connection = url.openConnection() as HttpURLConnection
            connection!!.connectTimeout = Constants.CONNECTION_TIMEOUT_MS
            connection.readTimeout = Constants.READ_TIMEOUT_MS
            connection.useCaches = false
            connection.requestMethod = mMethod
            connection.doOutput = mMethod == "POST"
            if ("https" == connection.url.protocol && mNeedClientAuth) {
                val httpsConnection =
                    connection as HttpsURLConnection?
                val cert =
                    nativeUserBindingCertificate()
                val pwd =
                    nativeUserBindingPassword()
                val decodedCert =
                    Base64.decode(cert, Base64.DEFAULT)
                val socketFactory: SSLSocketFactory =
                    ClientCertTLSSocketFactory.create(decodedCert, pwd.toCharArray())
                httpsConnection!!.sslSocketFactory = socketFactory
            }
            val fileSize: Long = StorageUtils.getFileSize(mFilePath)
            val paramsBuilder = StringBuilder()
            fillBodyParams(paramsBuilder)
            val file = File(mFilePath)
            fillFileParams(paramsBuilder, mFileKey, file)
            val endPartSize = mEndPart.toByteArray().size
            val paramsSize = paramsBuilder.toString().toByteArray().size
            val bodyLength = paramsSize + fileSize + endPartSize
            setStreamingMode(connection, bodyLength)
            setHeaders(connection, bodyLength)
            val startTime = System.currentTimeMillis()
            LOGGER.d(
                TAG,
                "Start bookmarks upload on url: '" + makeUrlSafe(mUrl) + "'"
            )
            val outputStream = connection.outputStream
            writer = PrintWriter(
                OutputStreamWriter(
                    outputStream,
                    CHARSET
                )
            )
            writeParams(writer, paramsBuilder)
            writeFileContent(outputStream, writer, file)
            writeEndPart(writer)
            status = connection.responseCode
            LOGGER.d(
                TAG,
                "Upload bookmarks status code: $status"
            )
            reader = BufferedReader(InputStreamReader(connection.inputStream))
            message = readResponse(reader)
            val duration = (System.currentTimeMillis() - startTime) / 1000
            LOGGER.d(
                TAG,
                "Upload bookmarks response: '" + message + "', " +
                        "duration = " + duration + " sec, body size = " + bodyLength + " bytes."
            )
        } catch (e: IOException) {
            message = "I/O exception '" + makeUrlSafe(mUrl) + "'"
            if (connection != null) {
                val errMsg = readErrorResponse(connection)
                if (!TextUtils.isEmpty(errMsg)) message = errMsg!!
            }
            LOGGER.e(
                TAG,
                message,
                e
            )
        } finally {
            closeSafely(writer!!)
            closeSafely(reader!!)
            connection?.disconnect()
        }
        return Result(status, message)
    }

    @Throws(IOException::class)
    private fun readResponse(reader: BufferedReader): String {
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) response.append(line)
        return response.toString()
    }

    private fun readErrorResponse(connection: HttpURLConnection): String? {
        var reader: BufferedReader? = null
        try {
            val errStream = connection.errorStream ?: return null
            reader = BufferedReader(
                InputStreamReader(connection.errorStream)
            )
            return readResponse(reader)
        } catch (e: IOException) {
            LOGGER.e(
                TAG,
                "Failed to read a error stream."
            )
        } finally {
            closeSafely(reader!!)
        }
        return null
    }

    private fun writeParams(
        writer: PrintWriter,
        paramsBuilder: StringBuilder
    ) {
        writer.append(paramsBuilder)
        writer.flush()
    }

    private fun setHeaders(
        connection: URLConnection,
        bodyLength: Long
    ) {
        mHeaders.add(KeyValue(HttpClient.HEADER_USER_AGENT, Framework.nativeGetUserAgent()))
        mHeaders.add(KeyValue("App-Version", BuildConfig.VERSION_NAME))
        mHeaders.add(KeyValue("Content-Type", "multipart/form-data; boundary=$mBoundary"))
        mHeaders.add(KeyValue("Content-Length", bodyLength.toString()))
        for (header in mHeaders) connection.setRequestProperty(header.mKey, header.mValue)
    }

    private fun fillBodyParams(builder: StringBuilder) {
        for (field in mParams) addParam(builder, field.mKey, field.mValue)
    }

    private fun addParam(
        builder: StringBuilder,
        key: String,
        value: String
    ) {
        builder.append("--").append(mBoundary)
            .append(LINE_FEED)
        builder.append("Content-Disposition: form-data; name=\"")
            .append(key)
            .append("\"")
            .append(LINE_FEED)
        builder.append(LINE_FEED)
        builder.append(value).append(LINE_FEED)
    }

    private fun fillFileParams(
        builder: StringBuilder, fieldName: String,
        uploadFile: File
    ) {
        val fileName = uploadFile.name
        builder.append("--").append(mBoundary)
            .append(LINE_FEED)
        builder.append("Content-Disposition: form-data; name=\"")
            .append(fieldName)
            .append("\"; filename=\"")
            .append(fileName)
            .append("\"")
            .append(LINE_FEED)
        builder.append("Content-Type: ")
            .append(URLConnection.guessContentTypeFromName(fileName))
            .append(LINE_FEED)
        builder.append(LINE_FEED)
    }

    @Throws(IOException::class)
    private fun writeFileContent(
        outputStream: OutputStream, writer: PrintWriter,
        uploadFile: File
    ) {
        val inputStream = FileInputStream(uploadFile)
        val size = Math.min(
            uploadFile.length().toInt(),
            BUFFER
        )
        val buffer = ByteArray(size)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) outputStream.write(
            buffer,
            0,
            bytesRead
        )
        closeSafely(inputStream)
    }

    private fun writeEndPart(writer: PrintWriter) {
        writer.append(mEndPart)
        writer.flush()
    }

    class Result internal constructor(
        private val mHttpCode: Int,
        private val mDescription: String
    ) {
        fun getHttpCode(): Int {
            return mHttpCode
        }

        fun getDescription(): String {
            return mDescription
        }

    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.NETWORK)
        private val TAG =
            HttpUploader::class.java.simpleName
        private const val LINE_FEED = "\r\n"
        private const val CHARSET = "UTF-8"
        private const val BUFFER = 8192
        private const val STATUS_CODE_UNKNOWN = -1
        private fun setStreamingMode(
            connection: HttpURLConnection,
            bodyLength: Long
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                connection.setFixedLengthStreamingMode(bodyLength)
                return
            }
            if (bodyLength <= Integer.MAX_VALUE) connection.setFixedLengthStreamingMode(bodyLength.toInt()) else connection.setChunkedStreamingMode(
                BUFFER
            )
        }

        @JvmStatic external fun nativeUserBindingCertificate(): String
        @JvmStatic external fun nativeUserBindingPassword(): String
    }

    init {
        mBoundary = "----" + System.currentTimeMillis()
        mParams = params.toList()
        mHeaders = headers.toMutableList()
        mEndPart =
            "$LINE_FEED--$mBoundary--$LINE_FEED"
        mNeedClientAuth = needClientAuth
    }
}