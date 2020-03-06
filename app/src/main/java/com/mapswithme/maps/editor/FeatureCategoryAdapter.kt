package com.mapswithme.maps.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.editor.FeatureCategoryAdapter.FeatureViewHolder
import com.mapswithme.maps.editor.data.FeatureCategory
import com.mapswithme.util.UiUtils

class FeatureCategoryAdapter(
    private val mFragment: FeatureCategoryFragment,
    private var mCategories: Array<FeatureCategory>,
    private val mSelectedCategory: FeatureCategory?
) : RecyclerView.Adapter<FeatureViewHolder>() {
    fun setCategories(categories: Array<FeatureCategory>) {
        mCategories = categories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        return FeatureViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_feature_category,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return mCategories.size
    }

    inner class FeatureViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val mName: TextView
        private val mSelected: View
        fun bind(position: Int) {
            mName.text = mCategories[position]!!.localizedTypeName
            val showCondition = (mSelectedCategory != null
                    && mCategories[position]!!.type == mSelectedCategory.type)
            UiUtils.showIf(showCondition, mSelected)
        }

        init {
            mName = itemView.findViewById(R.id.name)
            mSelected = itemView.findViewById(R.id.selected)
            UiUtils.hide(mSelected)
            itemView.setOnClickListener { v: View? ->
                onCategorySelected(
                    adapterPosition
                )
            }
        }
    }

    private fun onCategorySelected(adapterPosition: Int) {
        mFragment.selectCategory(mCategories[adapterPosition])
    }

}