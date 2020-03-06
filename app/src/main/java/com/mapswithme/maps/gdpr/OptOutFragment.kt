package com.mapswithme.maps.gdpr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.util.statistics.Statistics

class OptOutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Statistics.INSTANCE.trackSettingsDetails()
        return inflater.inflate(R.layout.fragment_gdpr, container, false)
    }
}