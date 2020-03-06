package com.mapswithme.maps.tips

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import android.view.View

import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.menu.MainMenu
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

import java.util.Arrays

enum class Tutorial private constructor(
    @param:StringRes @field:StringRes
    private val mPrimaryText: Int, @param:StringRes @field:StringRes
    private val mSecondaryText: Int, @param:IdRes @field:IdRes
    private val mAnchorViewId: Int,
    val siblingMenuItem: MainMenu.Item?, vararg allowedScreens: Class<*>
) {
    BOOKMARKS(
        R.string.tips_bookmarks_catalog_title,
        R.string.tips_bookmarks_catalog_message,
        R.id.bookmarks, MainMenu.Item.BOOKMARKS, MwmActivity::class.java
    ) {
        override fun createClickInterceptor(): ClickInterceptor {
            return ClickInterceptorFactory.createOpenBookmarksCatalogListener()
        }
    },

    SEARCH(
        R.string.tips_book_hotel_title,
        R.string.tips_book_hotel_message,
        R.id.search, MainMenu.Item.SEARCH, MwmActivity::class.java
    ) {
        override fun createClickInterceptor(): ClickInterceptor {
            return ClickInterceptorFactory.createSearchHotelsListener()
        }
    },

    DISCOVERY(
        R.string.tips_discover_button_title,
        R.string.tips_discover_button_message,
        R.id.discovery, MainMenu.Item.DISCOVERY, MwmActivity::class.java
    ) {
        override fun createClickInterceptor(): ClickInterceptor {
            return ClickInterceptorFactory.createOpenDiscoveryScreenListener()
        }
    },

    MAP_LAYERS(
        R.string.tips_map_layers_title,
        R.string.tips_map_layers_message,
        R.id.subway, null, MwmActivity::class.java
    ) {

        override fun createClickInterceptor(): ClickInterceptor {
            return ClickInterceptorFactory.createActivateSubwayLayerListener()
        }
    },

    STUB {
        override fun show(
            activity: Activity,
            listener: MaterialTapTargetPrompt.PromptStateChangeListener?
        ) {
            throw UnsupportedOperationException("Not supported here!")
        }

        override fun createClickInterceptor(): ClickInterceptor {
            return object : ClickInterceptor {
                override fun onInterceptClick(activity: MwmActivity) {

                }
            }
        }
    };

    private val mAllowedScreens: List<Class<*>>

    init {
        mAllowedScreens = Arrays.asList(*allowedScreens)
    }

    private constructor() : this(UiUtils.NO_ID, UiUtils.NO_ID, UiUtils.NO_ID, null) {}

    private fun isScreenAllowed(screenClass: Class<*>): Boolean {
        return mAllowedScreens.contains(screenClass)
    }

    open fun show(
        activity: Activity,
        listener: MaterialTapTargetPrompt.PromptStateChangeListener?
    ) {
        val target = activity.findViewById<View>(mAnchorViewId)
        val builder = MaterialTapTargetPrompt.Builder(activity)
            .setTarget(target)
            .setFocalRadius(R.dimen.focal_radius)
            .setPrimaryText(mPrimaryText)
            .setPrimaryTextSize(R.dimen.text_size_toolbar)
            .setPrimaryTextColour(ThemeUtils.getColor(activity, R.attr.tipsPrimaryTextColor))
            .setPrimaryTextTypeface(Typeface.DEFAULT_BOLD)
            .setSecondaryText(mSecondaryText)
            .setSecondaryTextColour(ThemeUtils.getColor(activity, R.attr.tipsSecondaryTextColor))
            .setSecondaryTextSize(R.dimen.text_size_body_3)
            .setSecondaryTextTypeface(Typeface.DEFAULT)
            .setBackgroundColour(ThemeUtils.getColor(activity, R.attr.tipsBgColor))
            .setFocalColour(activity.resources.getColor(android.R.color.transparent))
            .setPromptBackground(ImmersiveModeCompatPromptBackground(activity.windowManager))
            .setPromptStateChangeListener(listener)
        builder.show()
    }

    abstract fun createClickInterceptor(): ClickInterceptor

    companion object {

        private val LOGGER = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = Tutorial::class.java.simpleName

        fun <T> requestCurrent(
            context: Context,
            requiredScreenClass: Class<T>
        ): Tutorial {
            if (MwmApplication.from(context).isFirstLaunch)
                return STUB

            val index = Framework.nativeGetCurrentTipIndex()
            val value = if (index >= 0) values()[index] else STUB
            val tipsApi = if (value !== STUB && value.isScreenAllowed(requiredScreenClass))
                value
            else
                STUB
            LOGGER.d(TAG, "tipsApi = $tipsApi")
            return tipsApi
        }
    }
}
