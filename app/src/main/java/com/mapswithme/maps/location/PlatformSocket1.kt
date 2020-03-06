package com.mapswithme.maps.location

import android.annotation.SuppressLint
import android.net.SSLCertificateSocketFactory
import android.os.SystemClock
import com.mapswithme.maps.BuildConfig
import com.mapswithme.util.log.LoggerFactory
import java.io.IOException
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

/**
 * Implements interface that will be used by the core for
 * sending/receiving the raw data trough platform socket interface.
 *
 *
 * The instance of this class is supposed to be created in JNI layer
 * and supposed to be used in the thread safe environment, i.e. thread safety
 * should be provided externally (by the client of this class).
 *
 *
 * **All public methods are blocking and shouldn't be called from the main thread.**
 */
internal class PlatformSocket {
    private var mSocket: Socket? = null
    private var mHost: String? = null
    private var mPort = 0
    private var mTimeout = DEFAULT_TIMEOUT
    fun open(host: String, port: Int): Boolean {
        if (mSocket != null) {
            LOGGER.e(
                TAG,
                "Socket is already opened. Seems that it wasn't closed."
            )
            return false
        }
        if (!isPortAllowed(port)) {
            LOGGER.e(
                TAG,
                "A wrong port number = $port, it must be within (0-65535) range"
            )
            return false
        }
        mHost = host
        mPort = port
        val socket = createSocket(host, port, true)
        if (socket != null && socket.isConnected) {
            setReadSocketTimeout(socket, mTimeout)
            mSocket = socket
        }
        return mSocket != null
    }

    fun close() {
        if (mSocket == null) {
            LOGGER.d(
                TAG,
                "Socket is already closed or it wasn't opened yet\n"
            )
            return
        }
        try {
            mSocket!!.close()
            LOGGER.d(
                TAG,
                "Socket has been closed: $this\n"
            )
        } catch (e: IOException) {
            LOGGER.e(
                TAG,
                "Failed to close socket: $this\n"
            )
        } finally {
            mSocket = null
        }
    }

    fun read(data: ByteArray, count: Int): Boolean {
        if (!checkSocketAndArguments(data, count)) return false
        LOGGER.d(
            TAG,
            "Reading method is started, data.length = " + data.size + ", count = " + count
        )
        val startTime = SystemClock.elapsedRealtime()
        var readBytes = 0
        try {
            if (mSocket == null) throw AssertionError("mSocket cannot be null")
            val `in` = mSocket!!.getInputStream()
            while (readBytes != count && SystemClock.elapsedRealtime() - startTime < mTimeout) {
                try {
                    LOGGER.d(
                        TAG,
                        "Attempting to read $count bytes from offset = $readBytes"
                    )
                    val read = `in`.read(data, readBytes, count - readBytes)
                    if (read == -1) {
                        LOGGER.d(
                            TAG,
                            "All data is read from the stream, read bytes count = $readBytes\n"
                        )
                        break
                    }
                    if (read == 0) {
                        LOGGER.e(
                            TAG,
                            "0 bytes are obtained. It's considered as error\n"
                        )
                        break
                    }
                    LOGGER.d(
                        TAG,
                        "Read bytes count = $read\n"
                    )
                    readBytes += read
                } catch (e: SocketTimeoutException) {
                    val readingTime =
                        SystemClock.elapsedRealtime() - startTime
                    LOGGER.e(
                        TAG,
                        "Socked timeout has occurred after $readingTime (ms)\n "
                    )
                    if (readingTime > mTimeout) {
                        LOGGER.e(
                            TAG,
                            "Socket wrapper timeout has occurred, requested count = " +
                                    (count - readBytes) + ", readBytes = " + readBytes + "\n"
                        )
                        break
                    }
                }
            }
        } catch (e: IOException) {
            LOGGER.e(
                TAG,
                "Failed to read data from socket: $this\n"
            )
        }
        return count == readBytes
    }

    fun write(data: ByteArray, count: Int): Boolean {
        if (!checkSocketAndArguments(data, count)) return false
        LOGGER.d(
            TAG,
            "Writing method is started, data.length = " + data.size + ", count = " + count
        )
        val startTime = SystemClock.elapsedRealtime()
        try {
            if (mSocket == null) throw AssertionError("mSocket cannot be null")
            val out = mSocket!!.getOutputStream()
            out.write(data, 0, count)
            LOGGER.d(
                TAG,
                "$count bytes are written\n"
            )
            return true
        } catch (e: SocketTimeoutException) {
            val writingTime = SystemClock.elapsedRealtime() - startTime
            LOGGER.e(
                TAG,
                "Socked timeout has occurred after $writingTime (ms)\n"
            )
        } catch (e: IOException) {
            LOGGER.e(
                TAG,
                "Failed to write data to socket: $this\n"
            )
        }
        return false
    }

    private fun checkSocketAndArguments(data: ByteArray, count: Int): Boolean {
        if (mSocket == null) {
            LOGGER.e(
                TAG,
                "Socket must be opened before reading/writing\n"
            )
            return false
        }
        if (count < 0 || count > data.size) {
            LOGGER.e(
                TAG,
                "Illegal arguments, data.length = " + data.size + ", count = " + count + "\n"
            )
            return false
        }
        return true
    }

    fun setTimeout(millis: Int) {
        mTimeout = millis
        LOGGER.d(
            TAG,
            "Setting the socket wrapper timeout = $millis ms\n"
        )
    }

    private fun setReadSocketTimeout(socket: Socket, millis: Int) {
        try {
            socket.soTimeout = millis
        } catch (e: SocketException) {
            LOGGER.e(
                TAG,
                "Failed to set system socket timeout: " + millis + "ms, " + this + "\n"
            )
        }
    }

    override fun toString(): String {
        return "PlatformSocket{" +
                "mSocket=" + mSocket +
                ", mHost='" + mHost + '\'' +
                ", mPort=" + mPort +
                '}'
    }

    companion object {
        private const val DEFAULT_TIMEOUT = 30 * 1000
        private val TAG = PlatformSocket::class.java.simpleName
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.GPS_TRACKING)
        @Volatile
        private var sSslConnectionCounter: Long = 0

        private fun isPortAllowed(port: Int): Boolean {
            return port >= 0 && port <= 65535
        }

        private fun createSocket(
            host: String,
            port: Int,
            ssl: Boolean
        ): Socket? {
            return if (ssl) createSslSocket(
                host,
                port
            ) else createRegularSocket(host, port)
        }

        private fun createSslSocket(host: String, port: Int): Socket? {
            var socket: Socket? = null
            try {
                val sf = socketFactory
                socket = sf.createSocket(host, port)
                sSslConnectionCounter++
                LOGGER.d(
                    TAG,
                    "###############################################################################"
                )
                LOGGER.d(
                    TAG,
                    "$sSslConnectionCounter ssl connection is established."
                )
            } catch (e: IOException) {
                LOGGER.e(
                    TAG,
                    "Failed to create the ssl socket, mHost = $host mPort = $port"
                )
            }
            return socket
        }

        private fun createRegularSocket(host: String, port: Int): Socket? {
            var socket: Socket? = null
            try {
                socket = Socket(host, port)
                LOGGER.d(
                    TAG,
                    "Regular socket is created and tcp handshake is passed successfully"
                )
            } catch (e: IOException) {
                LOGGER.e(
                    TAG,
                    "Failed to create the socket, mHost = $host mPort = $port"
                )
            }
            return socket
        }//TODO: implement the custom KeyStore to make the self-signed certificates work

        // Trusting to any ssl certificate factory that will be used in
// debug mode, for testing purposes only.
        @get:SuppressLint("SSLCertificateSocketFactoryGetInsecure")
        private val socketFactory: SocketFactory
            private get() =// Trusting to any ssl certificate factory that will be used in
// debug mode, for testing purposes only.
                if (BuildConfig.DEBUG) SSLCertificateSocketFactory.getInsecure(
                    0,
                    null
                ) else SSLSocketFactory.getDefault()
    }

    init {
        LOGGER.d(
            TAG,
            "***********************************************************************************"
        )
        LOGGER.d(
            TAG,
            "Platform socket is created by core, ssl connection counter is discarded."
        )
    }
}