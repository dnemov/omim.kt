package com.mapswithme.maps.purchase

import android.net.Uri
import android.text.TextUtils
import com.mapswithme.maps.bookmarks.data.PaymentData
import com.mapswithme.util.CrashlyticsUtils
import com.mapswithme.util.log.LoggerFactory

class BookmarkPaymentDataParser : PaymentDataParser {
    override fun parse(url: String): PaymentData {
        val uri = Uri.parse(url)
        val serverId = getQueryRequiredParameter(
            uri,
            SERVER_ID
        )
        val productId =
            getQueryRequiredParameter(
                uri,
                PRODUCT_ID
            )
        val name = getQueryRequiredParameter(
            uri,
            NAME
        )
        val authorName =
            getQueryRequiredParameter(
                uri,
                AUTHOR_NAME
            )
        val group = PurchaseUtils.getTargetBookmarkGroupFromUri(uri)
        LOGGER.i(
            TAG,
            "Found target group: $group"
        )
        val imgUrl =
            uri.getQueryParameter(IMG_URL)
        return PaymentData(serverId, productId, name, imgUrl, authorName, group)
    }

    override fun getParameterByName(
        url: String,
        name: String
    ): String? {
        val uri = Uri.parse(url)
        return uri.getQueryParameter(name)
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
        private val TAG = BookmarkPaymentDataParser::class.java.simpleName
        const val SERVER_ID = "id"
        const val PRODUCT_ID = "tier"
        const val NAME = "name"
        const val IMG_URL = "img"
        const val AUTHOR_NAME = "author_name"
        private fun getQueryRequiredParameter(
            uri: Uri,
            name: String
        ): String {
            val parameter = uri.getQueryParameter(name)
            if (TextUtils.isEmpty(parameter)) {
                CrashlyticsUtils.logException(
                    IllegalArgumentException("'$name' parameter is required! URI: $uri")
                )
                return ""
            }
            return parameter!!
        }
    }
}