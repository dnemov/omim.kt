package com.mapswithme.maps.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.util.StringUtils
import java.util.*

class CuisineAdapter :
    RecyclerView.Adapter<CuisineAdapter.ViewHolder>() {
    private class Item(var cuisineKey: String, var cuisineTranslated: String) :
        Comparable<Item> {
        override fun compareTo(other: Item): Int {
            return cuisineTranslated.compareTo(other.cuisineTranslated)
        }

    }

    private val mItems: MutableList<Item> =
        ArrayList()
    private val mSelectedKeys: MutableSet<String> =
        HashSet()
    private var mFilter: String? = null
    fun setFilter(filter: String) {
        if (filter == mFilter) return
        mFilter = filter
        val filteredKeys =
            StringUtils.nativeFilterContainsNormalized(
                Editor.nativeGetCuisines(),
                filter
            )
        val filteredValues =
            Editor.nativeTranslateCuisines(filteredKeys)
        mItems.clear()
        if (filteredKeys != null)
            for (i in filteredKeys.indices) mItems.add(
                Item(
                    filteredKeys[i],
                    filteredValues!![i]
                )
            )
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_cuisine,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    val cuisines: Array<String>
        get() = mSelectedKeys.toTypedArray()

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CompoundButton.OnCheckedChangeListener {
        val cuisine: TextView
        val selected: CheckBox
        fun bind(position: Int) {
            val text = mItems[position].cuisineTranslated
            cuisine.text = text
            selected.setOnCheckedChangeListener(null)
            selected.isChecked = mSelectedKeys.contains(mItems[position].cuisineKey)
            selected.setOnCheckedChangeListener(this)
        }

        override fun onCheckedChanged(
            buttonView: CompoundButton,
            isChecked: Boolean
        ) {
            val key = mItems[adapterPosition].cuisineKey
            if (isChecked) mSelectedKeys.add(key) else mSelectedKeys.remove(key)
        }

        init {
            cuisine = itemView.findViewById<View>(R.id.cuisine) as TextView
            selected = itemView.findViewById<View>(R.id.selected) as CheckBox
            selected.setOnCheckedChangeListener(this)
            itemView.setOnClickListener { selected.toggle() }
        }
    }

    init {
        val keys =
            Editor.nativeGetCuisines()
        val selectedKeys =
            Editor.nativeGetSelectedCuisines()
        val translations =
            Editor.nativeTranslateCuisines(keys)
        Arrays.sort(selectedKeys)
        for (i in keys!!.indices) {
            val key = keys[i]
            mItems.add(Item(key, translations!![i]))
            if (Arrays.binarySearch(selectedKeys, key) >= 0) mSelectedKeys.add(key)
        }
    }
}