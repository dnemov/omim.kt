package com.mapswithme.maps.search

import android.app.Activity
import androidx.annotation.StringRes
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.util.ConnectionState
import java.util.*

enum class PromoCategory {
    MEGAFON {

        override val key: String
            get() = "megafon"
        override val stringId: Int
            get() = R.string.megafon
        override val provider: String
            get() = "Megafon"
        override val position: Int
            get() = 6
        override val isSupported: Boolean
            get() = ConnectionState.isConnected && Framework.nativeHasMegafonCategoryBanner()
        override val callToActionText: Int
            get() = R.string.details

        override fun createProcessor(activity: Activity): PromoCategoryProcessor {
            return MegafonPromoProcessor(activity)
        }
    };

    abstract val key: String
    @get:StringRes
    abstract val stringId: Int

    abstract val provider: String
    abstract val position: Int
    abstract val isSupported: Boolean
    @get:StringRes
    abstract val callToActionText: Int

    abstract fun createProcessor(activity: Activity): PromoCategoryProcessor

    companion object {
        fun findByStringId(@StringRes nameId: Int): PromoCategory? {
            for (category in values()) {
                if (category.stringId == nameId) return category
            }
            return null
        }

        fun supportedValues(): List<PromoCategory> {
            val result: MutableList<PromoCategory> =
                ArrayList()
            for (category in values()) {
                if (category.isSupported) result.add(category)
            }
            return result
        }
    }
}