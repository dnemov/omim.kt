package com.mapswithme.maps.search

import android.app.Activity
import com.mapswithme.maps.Framework
import com.mapswithme.util.Utils

class MegafonPromoProcessor internal constructor(private val mActivity: Activity) :
    PromoCategoryProcessor {
    override fun process() {
        Utils.openUrl(mActivity, Framework.nativeGetMegafonCategoryBannerUrl())
    }

}