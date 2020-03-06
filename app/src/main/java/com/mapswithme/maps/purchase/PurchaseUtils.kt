package com.mapswithme.maps.purchase

import android.net.Uri
import android.text.TextUtils
import androidx.fragment.app.Fragment
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.mapswithme.maps.R
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.util.CrashlyticsUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.log.LoggerFactory
import org.json.JSONException
import org.json.JSONObject
import java.util.*

object PurchaseUtils {
    const val GROUPS = "groups"
    const val REQ_CODE_PRODUCT_DETAILS_FAILURE = 1
    const val REQ_CODE_PAYMENT_FAILURE = 2
    const val REQ_CODE_VALIDATION_SERVER_ERROR = 3
    const val REQ_CODE_START_TRANSACTION_FAILURE = 4
    const val REQ_CODE_PING_FAILURE = 5
    const val REQ_CODE_CHECK_INVALID_SUBS_DIALOG = 6
    const val REQ_CODE_BMK_SUBS_SUCCESS_DIALOG = 7
    const val REQ_CODE_PAY_CONTINUE_SUBSCRIPTION = 8
    const val REQ_CODE_PAY_BOOKMARK = 9
    const val REQ_CODE_PAY_SUBSCRIPTION = 10
    const val DIALOG_TAG_CHECK_INVALID_SUBS = "check_invalid_subs"
    const val DIALOG_TAG_BMK_SUBSCRIPTION_SUCCESS = "bmk_subscription_success"
    const val EXTRA_IS_SUBSCRIPTION = "extra_is_subscription"
    const val WEEKS_IN_YEAR = 52
    const val MONTHS_IN_YEAR = 12
    private val LOGGER =
        LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
    private val TAG = PurchaseUtils::class.java.simpleName
    const val REQ_CODE_NO_NETWORK_CONNECTION_DIALOG = 11
    const val NO_NETWORK_CONNECTION_DIALOG_TAG = "no_network_connection_dialog_tag"
    fun parseToken(purchaseData: String): String {
        return try {
            Purchase(purchaseData, null).purchaseToken
        } catch (e: JSONException) {
            throw IllegalArgumentException("Failed to parse purchase token!")
        }
    }

    @kotlin.jvm.JvmStatic
    fun parseOrderId(purchaseData: String): String {
        return try {
            Purchase(purchaseData, null).orderId
        } catch (e: JSONException) {
            throw IllegalArgumentException("Failed to parse purchase order id!")
        }
    }

    fun toProductDetails(skuDetails: SkuDetails): ProductDetails {
        val price = normalizePrice(skuDetails.priceAmountMicros)
        val currencyCode = skuDetails.priceCurrencyCode
        return ProductDetails(skuDetails.sku, price, currencyCode, skuDetails.title)
    }

    fun toProductDetailsBundle(skuDetails: List<SkuDetails>): String {
        if (skuDetails.isEmpty()) return ""
        val bundleJson = JSONObject()
        for (details in skuDetails) {
            val priceJson = JSONObject()
            try {
                val price =
                    normalizePrice(details.priceAmountMicros)
                val currencyCode = details.priceCurrencyCode
                priceJson.put(
                    "price_string",
                    Utils.formatCurrencyString(price, currencyCode)
                )
                bundleJson.put(details.sku, priceJson)
            } catch (e: Exception) {
                val logger =
                    LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.BILLING)
                val tag = PurchaseUtils::class.java.simpleName
                val msg =
                    "Failed to form product details bundle for '$details': "
                logger.e(tag, msg, e)
                CrashlyticsUtils.logException(RuntimeException(msg, e))
                return ""
            }
        }
        return bundleJson.toString()
    }

    private fun normalizePrice(priceMicros: Long): Float {
        return priceMicros / 1000000f
    }

    fun hasIncorrectSkuDetails(skuDetails: List<SkuDetails>): Boolean {
        for (each in skuDetails) {
            if (Period.getInstance(each.subscriptionPeriod) == null) {
                val msg =
                    "Unsupported subscription period: '" + each.subscriptionPeriod + "'"
                CrashlyticsUtils.logException(IllegalStateException(msg))
                LOGGER.e(TAG, msg)
                return true
            }
        }
        return false
    }

    fun showPaymentFailureDialog(
        fragment: Fragment,
        tag: String?
    ) {
        val alertDialog =
            AlertDialog.Builder()
                .setReqCode(REQ_CODE_PAYMENT_FAILURE)
                .setTitleId(R.string.bookmarks_convert_error_title)
                .setMessageId(R.string.purchase_error_subtitle)
                .setPositiveBtnId(R.string.back)
                .build()
        alertDialog.show(fragment, tag)
    }

    fun showProductDetailsFailureDialog(
        fragment: Fragment,
        tag: String
    ) {
        val alertDialog =
            AlertDialog.Builder()
                .setReqCode(REQ_CODE_PRODUCT_DETAILS_FAILURE)
                .setTitleId(R.string.bookmarks_convert_error_title)
                .setMessageId(R.string.discovery_button_other_error_message)
                .setPositiveBtnId(R.string.ok)
                .build()
        alertDialog.show(fragment, tag)
    }

    fun showPingFailureDialog(fragment: Fragment) {
        val alertDialog =
            AlertDialog.Builder()
                .setReqCode(REQ_CODE_PING_FAILURE)
                .setTitleId(R.string.subscription_error_ping_title)
                .setMessageId(R.string.subscription_error_message)
                .setPositiveBtnId(R.string.ok)
                .build()
        alertDialog.show(fragment, null)
    }

    fun showNoConnectionDialog(fragment: Fragment) {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(R.string.common_check_internet_connection_dialog_title)
                .setMessageId(R.string.common_check_internet_connection_dialog)
                .setPositiveBtnId(R.string.try_again)
                .setNegativeBtnId(R.string.cancel)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .setReqCode(REQ_CODE_NO_NETWORK_CONNECTION_DIALOG)
                .build()
        dialog.setTargetFragment(fragment, REQ_CODE_NO_NETWORK_CONNECTION_DIALOG)
        dialog.show(fragment, NO_NETWORK_CONNECTION_DIALOG_TAG)
    }

    fun getTargetBookmarkGroupFromUri(uri: Uri): String {
        val uriGroups =
            uri.getQueryParameters(GROUPS)
        if (uriGroups == null || uriGroups.isEmpty()) {
            CrashlyticsUtils.logException(
                IllegalArgumentException(
                    "'" + GROUPS
                            + "' parameter is required! URI: " + uri
                )
            )
            return SubscriptionType.BOOKMARKS_ALL.serverId
        }
        val priorityGroups =
            Arrays.asList(
                SubscriptionType.BOOKMARKS_ALL.serverId,
                SubscriptionType.BOOKMARKS_SIGHTS.serverId
            )
        for (priorityGroup in priorityGroups) {
            for (uriGroup in uriGroups) {
                if (priorityGroup == uriGroup) {
                    return priorityGroup
                }
            }
        }
        return SubscriptionType.BOOKMARKS_ALL.serverId
    }

    enum class Period {
        // Order is important.
        P1Y,
        P1M, P1W;

        companion object {
            fun getInstance(subscriptionPeriod: String?): Period? {
                for (each in values()) {
                    if (TextUtils.equals(each.name, subscriptionPeriod)) return each
                }
                return null
            }
        }
    }
}