package com.mapswithme.maps.maplayer

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapswithme.maps.R
import com.mapswithme.maps.adapter.OnItemClickListener

import com.mapswithme.maps.maplayer.BottomSheetItem.Subway
import com.mapswithme.maps.maplayer.BottomSheetItem.Traffic
import com.mapswithme.maps.maplayer.subway.OnSubwayLayerToggleListener
import com.mapswithme.maps.maplayer.traffic.OnTrafficLayerToggleListener
import com.mapswithme.maps.widget.recycler.SpanningLinearLayoutManager
import java.util.*

class ToggleMapLayerDialog : DialogFragment() {
    private lateinit var mAdapter: ModeAdapter
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(activity!!)
        val inflater = activity!!.layoutInflater
        val root =
            inflater.inflate(R.layout.fragment_toggle_map_layer, null, false)
        dialog.setOnShowListener { dialogInterface: DialogInterface ->
            onShow(
                dialogInterface
            )
        }
        dialog.setContentView(root)
        initChildren(root)
        return dialog
    }

    private fun onShow(dialogInterface: DialogInterface) {
        val dialog = dialogInterface as BottomSheetDialog
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(
            bottomSheet!!
        )
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun initChildren(root: View) {
        initCloseBtn(root)
        initRecycler(root)
    }

    private fun initCloseBtn(root: View) {
        val closeBtn = root.findViewById<View>(R.id.сlose_btn)
        closeBtn.setOnClickListener { v: View? -> dismiss() }
    }

    private fun initRecycler(root: View) {
        val recycler: RecyclerView = root.findViewById(R.id.recycler)
        val layoutManager: RecyclerView.LayoutManager = SpanningLinearLayoutManager(
            context!!,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recycler.layoutManager = layoutManager
        mAdapter = ModeAdapter(createItems())
        recycler.adapter = mAdapter
    }

    private fun createItems(): List<Pair<BottomSheetItem, OnItemClickListener<BottomSheetItem>>> {
        val subwayListener = SubwayItemClickListener()
        val subway =
            Pair<BottomSheetItem, OnItemClickListener<BottomSheetItem>>(
                Subway.makeInstance(context!!),
                subwayListener
            )
        val trafficListener = TrafficItemClickListener()
        val traffic =
            Pair<BottomSheetItem, OnItemClickListener<BottomSheetItem>>(
                Traffic.Companion.makeInstance(context!!),
                trafficListener
            )
        return listOf(traffic, subway).toList()
    }

    private class ModeAdapter(private val mItems: List<Pair<BottomSheetItem, OnItemClickListener<BottomSheetItem>>>) :
        RecyclerView.Adapter<ModeHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeHolder {
            val inflater = LayoutInflater.from(parent.context)
            val root =
                inflater.inflate(R.layout.item_bottomsheet_dialog, parent, false)
            return ModeHolder(root)
        }

        override fun onBindViewHolder(holder: ModeHolder, position: Int) {
            val context = holder.itemView.context
            val pair =
                mItems[position]
            val item = pair.first
            holder.mItem = item!!
            val isEnabled = item.mode.isEnabled(context)
            holder.mButton.isSelected = isEnabled
            holder.mTitle.isSelected = isEnabled
            holder.mTitle.setText(item.title)
            holder.mButton.setImageResource(if (isEnabled) item.enabledStateDrawable else item.disabledStateDrawable)
            holder.mListener = pair.second
        }

        override fun getItemCount(): Int {
            return mItems.size
        }

    }

    private class ModeHolder internal constructor(root: View) :
        RecyclerView.ViewHolder(root) {
        val mButton: ImageView
        val mTitle: TextView
        internal lateinit var mItem: BottomSheetItem
        internal lateinit var mListener: OnItemClickListener<BottomSheetItem>

        val item: BottomSheetItem
            get() = mItem

        val listener: OnItemClickListener<BottomSheetItem>
            get() = mListener

        private fun onItemClicked(v: View) {
            listener.onItemClick(v, item)
        }

        init {
            mButton = root.findViewById(R.id.btn)
            mTitle = root.findViewById(R.id.name)
            mButton.setOnClickListener { v: View ->
                onItemClicked(
                    v
                )
            }
        }
    }

    private inner abstract class DefaultClickListener :
        OnItemClickListener<BottomSheetItem> {
        override fun onItemClick(v: View, item: BottomSheetItem) {
            item.mode.toggle(context!!)
            onItemClickInternal(v, item)
            mAdapter.notifyDataSetChanged()
        }

        abstract fun onItemClickInternal(v: View, item: BottomSheetItem)
    }

    private inner class SubwayItemClickListener : DefaultClickListener() {
        override fun onItemClickInternal(
            v: View,
            item: BottomSheetItem
        ) {
            val listener =
                activity as OnSubwayLayerToggleListener?
            listener!!.onSubwayLayerSelected()
        }
    }

    private inner class TrafficItemClickListener : DefaultClickListener() {
        override fun onItemClickInternal(
            v: View,
            item: BottomSheetItem
        ) {
            val listener =
                activity as OnTrafficLayerToggleListener?
            listener!!.onTrafficLayerSelected()
        }
    }

    companion object {
        fun show(activity: AppCompatActivity) {
            val frag = ToggleMapLayerDialog()
            val tag = frag.javaClass.canonicalName
            val fm = activity.supportFragmentManager
            val oldInstance = fm.findFragmentByTag(tag)
            if (oldInstance != null) return
            fm.beginTransaction().add(frag, tag).commitAllowingStateLoss()
            fm.executePendingTransactions()
        }
    }
}