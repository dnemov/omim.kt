package com.mapswithme.maps.purchase

import android.content.Context
import com.android.billingclient.api.BillingClient.SkuType
import com.mapswithme.maps.PurchaseOperationObservable

object PurchaseFactory {
    @kotlin.jvm.JvmStatic
    fun createAdsRemovalPurchaseController(
        context: Context
    ): PurchaseController<PurchaseCallback> {
        return createSubscriptionPurchaseController(
            context,
            SubscriptionType.ADS_REMOVAL
        )
    }

    @kotlin.jvm.JvmStatic
    fun createBookmarksAllSubscriptionController(
        context: Context
    ): PurchaseController<PurchaseCallback> {
        return createSubscriptionPurchaseController(
            context,
            SubscriptionType.BOOKMARKS_ALL
        )
    }

    @kotlin.jvm.JvmStatic
    fun createBookmarksSightsSubscriptionController(
        context: Context
    ): PurchaseController<PurchaseCallback> {
        return createSubscriptionPurchaseController(
            context,
            SubscriptionType.BOOKMARKS_SIGHTS
        )
    }

    private fun createSubscriptionPurchaseController(
        context: Context, type: SubscriptionType
    ): PurchaseController<PurchaseCallback> {
        val billingManager: BillingManager<PlayStoreBillingCallback> =
            PlayStoreBillingManager(SkuType.SUBS)
        val observable = PurchaseOperationObservable.from(context)
        val validator: PurchaseValidator<ValidationCallback> = DefaultPurchaseValidator(observable)
        val productIds = type.productIds
        return SubscriptionPurchaseController(validator, billingManager, type, *productIds)
    }

    fun createBookmarkPurchaseController(
        context: Context, productId: String?, serverId: String?
    ): PurchaseController<PurchaseCallback> {
        val billingManager: BillingManager<PlayStoreBillingCallback> =
            PlayStoreBillingManager(SkuType.INAPP)
        val observable = PurchaseOperationObservable.from(context)
        val validator: PurchaseValidator<ValidationCallback> = DefaultPurchaseValidator(observable)
        return BookmarkPurchaseController(validator, billingManager, productId, serverId)
    }

    @kotlin.jvm.JvmStatic
    fun createFailedBookmarkPurchaseController(
        context: Context
    ): PurchaseController<FailedPurchaseChecker> {
        val billingManager: BillingManager<PlayStoreBillingCallback> =
            PlayStoreBillingManager(SkuType.INAPP)
        val observable = PurchaseOperationObservable.from(context)
        val validator: PurchaseValidator<ValidationCallback> = DefaultPurchaseValidator(observable)
        return FailedBookmarkPurchaseController(validator, billingManager)
    }

    fun createInAppBillingManager(): BillingManager<PlayStoreBillingCallback> {
        return PlayStoreBillingManager(SkuType.INAPP)
    }

    fun createSubscriptionBillingManager(): BillingManager<PlayStoreBillingCallback> {
        return PlayStoreBillingManager(SkuType.SUBS)
    }
}