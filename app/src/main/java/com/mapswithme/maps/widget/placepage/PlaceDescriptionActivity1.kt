package com.mapswithme.maps.widget.placepage

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseToolbarActivity
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import com.mapswithme.util.statistics.Statistics.ParameterBuilder

class PlaceDescriptionActivity : BaseToolbarActivity() {
    override val fragmentClass: Class<out Fragment?>
        protected get() = PlaceDescriptionFragment::class.java

    companion object {
        fun start(
            context: Context, description: String,
            source: String
        ) {
            val intent = Intent(context, PlaceDescriptionActivity::class.java)
                .putExtra(PlaceDescriptionFragment.Companion.EXTRA_DESCRIPTION, description)
            context.startActivity(intent)
            val builder = ParameterBuilder()
                .add(Statistics.EventParam.SOURCE, source)
            Statistics.INSTANCE.trackEvent(
                EventName.PLACEPAGE_DESCRIPTION_MORE,
                builder.get(),
                Statistics.STATISTICS_CHANNEL_REALTIME
            )
        }
    }
}