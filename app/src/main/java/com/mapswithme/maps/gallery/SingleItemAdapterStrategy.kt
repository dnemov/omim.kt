package com.mapswithme.maps.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R

abstract class SingleItemAdapterStrategy<T : Holders.BaseViewHolder<Items.Item>>(
    url: String?,
    listener: ItemSelectedListener<Items.Item>?
) : AdapterStrategy<T, Items.Item>(listener) {
    protected open fun buildItem(url: String?) {
        val res = MwmApplication.get().resources
        mItems.add(
            Items.Item(
                res.getString(title), url,
                res.getString(subtitle)
            )
        )
    }

    @get:StringRes
    protected abstract val title: Int

    @get:StringRes
    protected abstract val subtitle: Int

    protected abstract fun inflateView(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): View

    override fun createViewHolder(parent: ViewGroup, viewType: Int): T {
        val itemView =
            inflateView(LayoutInflater.from(parent.context), parent)
        val button = itemView.findViewById<View>(R.id.button) as TextView
        button.setText(labelForDetailsView)
        return createViewHolder(itemView)
    }

    protected abstract fun createViewHolder(itemView: View): T
    @get:StringRes
    protected abstract val labelForDetailsView: Int

    override fun onBindViewHolder(holder: Holders.BaseViewHolder<Items.Item>?, position: Int) {
        holder!!.bind(mItems[position])
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    init {
        buildItem(url)
    }
}