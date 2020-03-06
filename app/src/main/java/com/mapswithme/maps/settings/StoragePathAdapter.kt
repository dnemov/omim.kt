package com.mapswithme.maps.settings

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import com.mapswithme.maps.R
import java.util.*

internal class StoragePathAdapter(
    private val mStoragePathManager: StoragePathManager,
    context: Activity?
) : BaseAdapter() {
    private val mInflater: LayoutInflater
    private val mItems: MutableList<StorageItem>? =
        ArrayList()
    private var mCurrentStorageIndex = -1
    private var mSizeNeeded: Long = 0
    override fun getCount(): Int {
        return mItems?.size ?: 0
    }

    override fun getItem(position: Int): StorageItem {
        return mItems!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        if (convertView == null) convertView =
            mInflater.inflate(R.layout.item_storage, parent, false)
        val item = mItems!![position]
        val checkedView = convertView as CheckedTextView
        checkedView.text =
            item.mPath + ": " + StoragePathFragment.Companion.getSizeString(item.mFreeSize)
        checkedView.isChecked = position == mCurrentStorageIndex
        checkedView.isEnabled = position == mCurrentStorageIndex || isStorageBigEnough(position)
        return convertView
    }

    fun onItemClick(position: Int) {
        if (isStorageBigEnough(position) && position != mCurrentStorageIndex) mStoragePathManager.changeStorage(
            position
        )
    }

    fun update(
        items: List<StorageItem>?,
        currentItemIndex: Int,
        dirSize: Long
    ) {
        mSizeNeeded = dirSize
        mItems!!.clear()
        mItems.addAll(items!!)
        mCurrentStorageIndex = currentItemIndex
        notifyDataSetChanged()
    }

    private fun isStorageBigEnough(index: Int): Boolean {
        return mItems!![index].mFreeSize >= mSizeNeeded
    }

    init {
        mInflater = context!!.layoutInflater
    }
}