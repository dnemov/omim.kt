package com.mapswithme.maps.onboarding


import com.mapswithme.maps.news.OnboardingStep

object Utils {
    @kotlin.jvm.JvmStatic
    fun getOnboardingStepByTip(tip: OnboardingTip): OnboardingStep {
        return when (tip.type) {
            OnboardingTip.BUY_SUBSCRIPTION -> OnboardingStep.SUBSCRIBE_TO_CATALOG
            OnboardingTip.DISCOVER_CATALOG -> OnboardingStep.DISCOVER_GUIDES
            OnboardingTip.DOWNLOAD_SAMPLES -> OnboardingStep.CHECK_OUT_SIGHTS
            else -> throw UnsupportedOperationException("Unsupported onboarding tip: $tip")
        }
    }
}