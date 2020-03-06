package com.mapswithme.maps.routing

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.facebook.internal.Mutable
import com.mapswithme.maps.R
import com.mapswithme.maps.adapter.DisabledChildSimpleExpandableListAdapter
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.downloader.CountryItem
import com.mapswithme.maps.downloader.CountryItem.Companion.fill
import com.mapswithme.util.StringUtils
import com.mapswithme.util.UiUtils
import java.util.*

abstract class BaseRoutingErrorDialogFragment : BaseMwmDialogFragment() {
    @kotlin.jvm.JvmField
    val mMissingMaps: MutableList<CountryItem> =
        ArrayList()
    @kotlin.jvm.JvmField
    var mMapsArray: Array<String>? = null
    private var mCancelRoute = true
    @kotlin.jvm.JvmField
    var mCancelled = false
    open fun beforeDialogCreated(builder: AlertDialog.Builder?) {}
    open fun bindGroup(view: View?) {}
    private fun createDialog(builder: AlertDialog.Builder): Dialog {
        val view =
            if (mMissingMaps.size == 1) buildSingleMapView(mMissingMaps[0]) else buildMultipleMapView()!!
        builder.setView(view)
        return builder.create()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        parseArguments()
        val builder =
            AlertDialog.Builder(activity!!)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
        beforeDialogCreated(builder)
        return createDialog(builder)
    }

    override fun onStart() {
        super.onStart()
        val dlg =
            dialog as AlertDialog?
        dlg!!.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setOnClickListener {
                mCancelled = true
                dismiss()
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (mCancelled && mCancelRoute) RoutingController.get().cancel()
        super.onDismiss(dialog)
    }

    open fun parseArguments() {
        val args = arguments
        mMapsArray =
            args!!.getStringArray(EXTRA_MISSING_MAPS)
        for (map in mMapsArray!!) mMissingMaps.add(fill(map))
    }

    open fun buildSingleMapView(map: CountryItem): View {
        @SuppressLint("InflateParams") val countryView =
            View.inflate(activity, R.layout.dialog_missed_map, null)
        (countryView.findViewById<View>(R.id.tv__title) as TextView).text = map.name
        val szView =
            countryView.findViewById<View>(R.id.tv__size) as TextView
        szView.text = StringUtils.getFileSizeString(map.totalSize)
        val lp = szView.layoutParams as MarginLayoutParams
        lp.rightMargin = 0
        szView.layoutParams = lp
        return countryView
    }

    open fun buildMultipleMapView(): View? {
        @SuppressLint("InflateParams") val countriesView =
            View.inflate(activity, R.layout.dialog_missed_maps, null)
        val listView =
            countriesView.findViewById<View>(R.id.items_frame) as ExpandableListView
        if (mMissingMaps.isEmpty()) {
            mCancelRoute = false
            UiUtils.hide(listView)
            UiUtils.hide(countriesView.findViewById(R.id.divider_top))
            UiUtils.hide(countriesView.findViewById(R.id.divider_bottom))
            return countriesView
        }
        listView.setAdapter(buildAdapter())
        listView.setChildDivider(ColorDrawable(resources.getColor(android.R.color.transparent)))
        UiUtils.waitLayout(listView, ViewTreeObserver.OnGlobalLayoutListener {
            val width = listView.width
            val indicatorWidth = UiUtils.dimen(R.dimen.margin_quadruple)
            listView.setIndicatorBounds(width - indicatorWidth, width)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) listView.setIndicatorBounds(
                width - indicatorWidth,
                width
            ) else listView.setIndicatorBoundsRelative(width - indicatorWidth, width)
        })
        return countriesView
    }

    private fun buildAdapter(): ExpandableListAdapter {
        val countries: MutableList<MutableMap<String, String>> =
            ArrayList()
        var size: Long = 0
        for (item in mMissingMaps) {
            val data: MutableMap<String, String> =
                HashMap()
            data[COUNTRY_NAME] = item.name!!
            countries.add(data)
            size += item.totalSize
        }
        val group: MutableMap<String, String> =
            HashMap()
        group[GROUP_NAME] = getString(R.string.maps) + " (" + mMissingMaps.size + ") "
        group[GROUP_SIZE] = StringUtils.getFileSizeString(size)
        val groups: MutableList<MutableMap<String, String>> =
            ArrayList()
        groups.add(group)
        val children: MutableList<List<MutableMap<String, String>>> =
            ArrayList()
        children.add(countries)
        return object : DisabledChildSimpleExpandableListAdapter(
            activity,
            groups,
            R.layout.item_missed_map_group,
            R.layout.item_missed_map,
            arrayOf<String>(
                GROUP_NAME,
                GROUP_SIZE
            ),
            intArrayOf(R.id.tv__title, R.id.tv__size),
            children,
            R.layout.item_missed_map,
            arrayOf<String>(COUNTRY_NAME),
            intArrayOf(R.id.tv__title)
        ) {
            override fun getGroupView(
                groupPosition: Int,
                isExpanded: Boolean,
                convertView: View,
                parent: ViewGroup
            ): View {
                val res =
                    super.getGroupView(groupPosition, isExpanded, convertView, parent)
                bindGroup(res)
                return res
            }
        }
    }

    companion object {
        const val EXTRA_MISSING_MAPS = "MissingMaps"
        private const val GROUP_NAME = "GroupName"
        private const val GROUP_SIZE = "GroupSize"
        private const val COUNTRY_NAME = "CountryName"
    }
}