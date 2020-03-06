package com.mapswithme.maps.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

interface ResolveFragmentManagerStrategy {
    fun resolve(baseFragment: Fragment): FragmentManager
    fun resolve(activity: FragmentActivity): FragmentManager
}