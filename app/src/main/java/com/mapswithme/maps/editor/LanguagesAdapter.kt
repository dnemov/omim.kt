package com.mapswithme.maps.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.editor.data.Language

class LanguagesAdapter(
    private val mFragment: LanguagesFragment,
    private val mLanguages: Array<Language>
) : RecyclerView.Adapter<LanguagesAdapter.Holder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_language,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return mLanguages.size
    }

    inner class Holder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var name: TextView
        fun bind(position: Int) {
            name.text = mLanguages[position].name
        }

        init {
            name = itemView as TextView
            itemView.setOnClickListener {
                mFragment.onLanguageSelected(
                    mLanguages[adapterPosition]
                )
            }
        }
    }

}