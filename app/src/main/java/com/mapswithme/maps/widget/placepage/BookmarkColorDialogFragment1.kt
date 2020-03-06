package com.mapswithme.maps.widget.placepage

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import androidx.appcompat.app.AlertDialog
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.bookmarks.IconsAdapter
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.Icon

class BookmarkColorDialogFragment : BaseMwmDialogFragment() {
    private var mIconColor = 0

    interface OnBookmarkColorChangeListener {
        fun onBookmarkColorSet(colorPos: Int)
    }

    private var mColorSetListener: OnBookmarkColorChangeListener? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (arguments != null) mIconColor =
            arguments!!.getInt(ICON_TYPE)
        return AlertDialog.Builder(activity!!)
            .setView(buildView())
            .setTitle(R.string.bookmark_color)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
    }

    fun setOnColorSetListener(listener: OnBookmarkColorChangeListener?) {
        mColorSetListener = listener
    }

    private fun buildView(): View {
        val icons: List<Icon> =
            BookmarkManager.ICONS
        val adapter = IconsAdapter(activity, icons)
        adapter.chooseItem(mIconColor)
        val gView = LayoutInflater.from(activity).inflate(
            R.layout.fragment_color_grid,
            null
        ) as GridView
        gView.adapter = adapter
        gView.onItemClickListener = AdapterView.OnItemClickListener { arg0, who, pos, id ->
            if (mColorSetListener != null) mColorSetListener!!.onBookmarkColorSet(pos)
            dismiss()
        }
        return gView
    }

    companion object {
        const val ICON_TYPE = "ExtraIconType"
    }
}