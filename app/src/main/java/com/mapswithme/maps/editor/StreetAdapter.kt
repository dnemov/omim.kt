package com.mapswithme.maps.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.dialog.EditTextDialogFragment
import com.mapswithme.maps.editor.data.LocalizedStreet
import com.mapswithme.util.UiUtils

class StreetAdapter(
    private val mFragment: StreetFragment,
    private val mStreets: Array<LocalizedStreet>,
    selected: LocalizedStreet
) : RecyclerView.Adapter<StreetAdapter.BaseViewHolder>() {
    var selectedStreet: LocalizedStreet
        private set

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder {
        return if (viewType == TYPE_STREET) StreetViewHolder(
            LayoutInflater.from(
                parent.context
            ).inflate(R.layout.item_street, parent, false)
        ) else AddViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_add_street,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int
    ) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return mStreets.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) TYPE_ADD_STREET else TYPE_STREET
    }

    private fun addStreet() {
        val resources = MwmApplication.get().resources
        EditTextDialogFragment.show(
            resources.getString(R.string.street), null,
            resources.getString(R.string.ok),
            resources.getString(R.string.cancel), mFragment
        )
    }

    inner abstract class BaseViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        open fun bind(position: Int) {}
    }

    protected inner class StreetViewHolder(itemView: View) :
        BaseViewHolder(itemView),
        View.OnClickListener {
        val streetDef: TextView
        val streetLoc: TextView
        val selected: CompoundButton
        override fun bind(position: Int) {
            selected.isChecked = selectedStreet!!.defaultName == mStreets[position]!!.defaultName
            streetDef.text = mStreets[position]!!.defaultName
            UiUtils.setTextAndHideIfEmpty(streetLoc, mStreets[position]!!.localizedName)
        }

        override fun onClick(v: View) {
            selectedStreet = mStreets[adapterPosition]
            notifyDataSetChanged()
            mFragment.saveStreet(selectedStreet)
        }

        init {
            streetDef = itemView.findViewById<View>(R.id.street_default) as TextView
            streetLoc = itemView.findViewById<View>(R.id.street_localized) as TextView
            selected = itemView.findViewById<View>(R.id.selected) as CompoundButton
            itemView.setOnClickListener(this)
            selected.setOnClickListener {
                selected.toggle()
                onClick(selected)
            }
        }
    }

    protected inner class AddViewHolder(itemView: View) :
        BaseViewHolder(itemView) {
        init {
            itemView.setOnClickListener { addStreet() }
        }
    }

    companion object {
        private const val TYPE_ADD_STREET = 0
        private const val TYPE_STREET = 1
    }

    init {
        selectedStreet = selected
    }
}