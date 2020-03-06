package com.mapswithme.maps.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleExpandableListAdapter

/**
 * Disables child selections, also fixes bug with SimpleExpandableListAdapter not switching expandedGroupLayout and collapsedGroupLayout correctly.
 */
open class DisabledChildSimpleExpandableListAdapter : SimpleExpandableListAdapter {
    constructor(
        context: Context?,
        groupData: List<MutableMap<String?, *>>?,
        groupLayout: Int,
        groupFrom: Array<String>?,
        groupTo: IntArray?,
        childData: List<List<MutableMap<String?, *>>?>?,
        childLayout: Int,
        childFrom: Array<String>?,
        childTo: IntArray?
    ) : super(
        context,
        groupData,
        groupLayout,
        groupFrom,
        groupTo,
        childData,
        childLayout,
        childFrom,
        childTo
    ) {
    }

    constructor(
        context: Context?,
        groupData: List<MutableMap<String, *>>,
        expandedGroupLayout: Int,
        collapsedGroupLayout: Int,
        groupFrom: Array<String>?,
        groupTo: IntArray?,
        childData: List<List<MutableMap<String, *>>?>,
        childLayout: Int,
        childFrom: Array<String>?,
        childTo: IntArray?
    ) : super(
        context,
        groupData,
        expandedGroupLayout,
        collapsedGroupLayout,
        groupFrom,
        groupTo,
        childData,
        childLayout,
        childFrom,
        childTo
    ) {
    }

    constructor(
        context: Context?,
        groupData: List<MutableMap<String?, *>>?,
        expandedGroupLayout: Int,
        collapsedGroupLayout: Int,
        groupFrom: Array<String>?,
        groupTo: IntArray?,
        childData: List<List<MutableMap<String?, *>>?>?,
        childLayout: Int,
        lastChildLayout: Int,
        childFrom: Array<String>?,
        childTo: IntArray?
    ) : super(
        context,
        groupData,
        expandedGroupLayout,
        collapsedGroupLayout,
        groupFrom,
        groupTo,
        childData,
        childLayout,
        lastChildLayout,
        childFrom,
        childTo
    ) {
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return false
    }

    /*
   * Quick bugfix, pass convertView param null always to change expanded-collapsed groupview correctly.
   * See http://stackoverflow.com/questions/19520037/simpleexpandablelistadapter-and-expandedgrouplayout for details
   */
    override fun getGroupView(
        groupPosition: Int, isExpanded: Boolean, convertView: View,
        parent: ViewGroup
    ): View {
        return super.getGroupView(groupPosition, isExpanded, null, parent)
    }
}