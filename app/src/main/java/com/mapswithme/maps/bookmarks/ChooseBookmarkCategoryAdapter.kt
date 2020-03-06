package com.mapswithme.maps.bookmarks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.ChooseBookmarkCategoryAdapter.SingleChoiceHolder
import com.mapswithme.maps.bookmarks.data.BookmarkCategory

class ChooseBookmarkCategoryAdapter(
    context: Context?, private var mCheckedPosition: Int,
    categories: List<BookmarkCategory>
) : BaseBookmarkCategoryAdapter<SingleChoiceHolder?>(context!!, categories) {

    interface CategoryListener {
        fun onCategorySet(categoryPosition: Int)
        fun onCategoryCreate()
    }

    private var mListener: CategoryListener? = null
    fun setListener(listener: CategoryListener?) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleChoiceHolder {
        val view: View
        val inflater = LayoutInflater.from(parent.context)
        view = if (viewType == VIEW_TYPE_CATEGORY) inflater.inflate(
            R.layout.item_bookmark_category_choose,
            parent,
            false
        ) else inflater.inflate(R.layout.item_bookmark_category_create, parent, false)
        val holder = SingleChoiceHolder(view)
        view.setOnClickListener(View.OnClickListener {
            if (mListener == null) return@OnClickListener
            if (holder.itemViewType == VIEW_TYPE_ADD_NEW) mListener!!.onCategoryCreate() else mListener!!.onCategorySet(
                holder.adapterPosition
            )
        })
        return holder
    }

    override fun onBindViewHolder(holder: SingleChoiceHolder, position: Int) {
        if (holder.itemViewType == VIEW_TYPE_CATEGORY) {
            val category = getCategoryByPosition(position)
            holder.name.text = category.name
            holder.checked.isChecked = mCheckedPosition == position
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) VIEW_TYPE_ADD_NEW else VIEW_TYPE_CATEGORY
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    fun chooseItem(position: Int) {
        val oldPosition = mCheckedPosition
        mCheckedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(mCheckedPosition)
    }

    class SingleChoiceHolder(convertView: View) :
        RecyclerView.ViewHolder(convertView) {
        var name: TextView
        var checked: RadioButton

        init {
            name = convertView.findViewById<View>(R.id.tv__set_name) as TextView
            checked = convertView.findViewById<View>(R.id.rb__selected) as RadioButton
        }
    }

    companion object {
        const val VIEW_TYPE_CATEGORY = 0
        const val VIEW_TYPE_ADD_NEW = 1
    }

}