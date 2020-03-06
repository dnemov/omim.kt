package com.mapswithme.util

import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import com.mapswithme.util.Utils.closeSafely
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

object ClientCertTLSSocketFactory {
    private const val PROTOCOL = "TLS"
    private const val ALGORITHM = "X509"
    private const val KEY_STORE_TYPE = "PKCS12"
    fun create(payload: ByteArray, password: CharArray?): SSLSocketFactory {
        var inputStream: InputStream? = null
        return try {
            inputStream = ByteArrayInputStream(payload)
            val keyStore =
                KeyStore.getInstance(KEY_STORE_TYPE)
            keyStore.load(inputStream, password)
            val kmf =
                KeyManagerFactory.getInstance(ALGORITHM)
            kmf.init(keyStore, null)
            val sslContext =
                SSLContext.getInstance(PROTOCOL)
            sslContext.init(kmf.keyManagers, null, SecureRandom())
            sslContext.socketFactory
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        } finally {
            closeSafely(inputStream!!)
        }
    }
}