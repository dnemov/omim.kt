package com.mapswithme.maps.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.XmlRes
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import com.mapswithme.maps.R
import com.mapswithme.util.Config
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import org.alohalytics.Statistics

abstract class BaseXmlSettingsFragment : PreferenceFragmentCompat() {
    @get:XmlRes
    protected abstract val xmlResources: Int

    override fun onCreatePreferences(bundle: Bundle?, root: String?) {
        setPreferencesFromResource(xmlResources, root)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Utils.detachFragmentIfCoreNotInitialized(context, this)
    }

    override fun onResume() {
        super.onResume()
        Statistics.logEvent(
            "\$onResume", javaClass.simpleName + ":" +
                    UiUtils.deviceOrientationAsString(activity!!)
        )
    }

    override fun onPause() {
        super.onPause()
        Statistics.logEvent(
            "\$onPause", javaClass.simpleName + ":" +
                    UiUtils.deviceOrientationAsString(activity!!)
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val theme = Config.getCurrentUiTheme()
        val color: Int
        color = if (ThemeUtils.isDefaultTheme(theme)) ContextCompat.getColor(
            context!!,
            R.color.bg_cards
        ) else ContextCompat.getColor(context!!, R.color.bg_cards_night)
        view.setBackgroundColor(color)
    }

    protected val settingsActivity: SettingsActivity?
        get() = activity as SettingsActivity?
}