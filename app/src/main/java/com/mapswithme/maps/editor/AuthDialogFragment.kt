package com.mapswithme.maps.editor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment

class AuthDialogFragment : BaseMwmDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_auth_editor_dialog, container, false)
    }

    override val style: Int
        protected get() = STYLE_NO_TITLE

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val osmAuthDelegate: OsmAuthFragmentDelegate = object : OsmAuthFragmentDelegate(this) {
            override fun loginOsm() {
                startActivity(Intent(context, OsmAuthActivity::class.java))
                dismiss()
            }
        }
        osmAuthDelegate.onViewCreated(view, savedInstanceState)
    }
}