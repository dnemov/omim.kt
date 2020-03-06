package com.mapswithme.util

import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal class PreLollipopSSLSocketFactory(private val mSslSocketFactory: SSLSocketFactory) :
    SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> {
        return mSslSocketFactory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return mSslSocketFactory.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(
        s: Socket,
        host: String,
        port: Int,
        autoClose: Boolean
    ): SSLSocket {
        val socket =
            mSslSocketFactory.createSocket(s, host, port, autoClose) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        val socket =
            mSslSocketFactory.createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket {
        val socket = mSslSocketFactory.createSocket(
            host,
            port,
            localHost,
            localPort
        ) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        val socket =
            mSslSocketFactory.createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket {
        val socket = mSslSocketFactory.createSocket(
            address,
            port,
            localAddress,
            localPort
        ) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

}