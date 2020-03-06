package com.mapswithme.maps.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.databinding.FragmentBookmarksAllSubscriptionBinding
import java.util.*

class BookmarksAllSubscriptionPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val index = arguments!!.getInt(BUNDLE_INDEX)
        val page =
            BookmarksAllSubscriptionPage.values()[index]
        val binding =
            makeBinding(inflater, container)
        binding.page = page
        return binding.root
    }

    companion object {
        private const val BUNDLE_INDEX = "index"
        private fun makeBinding(
            inflater: LayoutInflater,
            container: ViewGroup?
        ): FragmentBookmarksAllSubscriptionBinding {
            return DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_bookmarks_all_subscription,
                container,
                false
            )
        }

        fun newInstance(index: Int): Fragment {
            val fragment =
                BookmarksAllSubscriptionPageFragment()
            val args = Bundle()
            args.putInt(BUNDLE_INDEX, index)
            fragment.arguments = args
            return fragment
        }
    }
}