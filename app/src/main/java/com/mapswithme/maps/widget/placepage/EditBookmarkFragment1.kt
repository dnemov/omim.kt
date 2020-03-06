package com.mapswithme.maps.widget.placepage

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.bookmarks.ChooseBookmarkCategoryFragment
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkInfo
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.Icon
import com.mapswithme.maps.widget.placepage.BookmarkColorDialogFragment.OnBookmarkColorChangeListener
import com.mapswithme.util.Graphics
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics

class EditBookmarkFragment : BaseMwmDialogFragment(),
    View.OnClickListener,
    ChooseBookmarkCategoryFragment.Listener {
    private var mEtDescription: EditText? = null
    private var mEtName: EditText? = null
    private var mTvBookmarkGroup: TextView? = null
    private var mIvColor: ImageView? = null
    private var mBookmarkCategory: BookmarkCategory? = null
    private var mIcon: Icon? = null
    private var mBookmark: BookmarkInfo? = null
    private var mListener: EditBookmarkListener? = null

    interface EditBookmarkListener {
        fun onBookmarkSaved(
            bookmarkId: Long,
            movedFromCategory: Boolean
        )
    }

    override val customTheme: Int
        get() = fullscreenTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_bookmark, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        val args = arguments
        val categoryId = args!!.getLong(EXTRA_CATEGORY_ID)
        mBookmarkCategory = BookmarkManager.INSTANCE.getCategoryById(categoryId)
        val bookmarkId = args.getLong(EXTRA_BOOKMARK_ID)
        mBookmark = BookmarkManager.INSTANCE.getBookmarkInfo(bookmarkId)
        if (mBookmark != null) mIcon = mBookmark!!.icon
        mEtName = view.findViewById<View>(R.id.et__bookmark_name) as EditText
        mEtDescription = view.findViewById<View>(R.id.et__description) as EditText
        mTvBookmarkGroup = view.findViewById<View>(R.id.tv__bookmark_set) as TextView
        mTvBookmarkGroup!!.setOnClickListener(this)
        mIvColor =
            view.findViewById<View>(R.id.iv__bookmark_color) as ImageView
        mIvColor!!.setOnClickListener(this)
        refreshBookmark()
        initToolbar(view)
    }

    private fun initToolbar(view: View) {
        val toolbar =
            view.findViewById<View>(R.id.toolbar) as Toolbar
        UiUtils.extendViewWithStatusBar(toolbar)
        val textView = toolbar.findViewById<View>(R.id.tv__save) as TextView
        textView.setOnClickListener { saveBookmark() }
        UiUtils.showHomeUpButton(toolbar)
        toolbar.setTitle(R.string.description)
        toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun saveBookmark() {
        if (mBookmark == null) {
            dismiss()
            return
        }
        val movedFromCategory = mBookmark!!.categoryId != mBookmarkCategory!!.id
        if (movedFromCategory) BookmarkManager.INSTANCE.notifyCategoryChanging(
            mBookmark!!,
            mBookmarkCategory!!.id
        )
        BookmarkManager.INSTANCE.notifyParametersUpdating(
            mBookmark!!, mEtName!!.text.toString(),
            mIcon, mEtDescription!!.text.toString()
        )
        if (mListener != null) mListener!!.onBookmarkSaved(
            mBookmark!!.bookmarkId,
            movedFromCategory
        )
        dismiss()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv__bookmark_color -> selectBookmarkColor()
            R.id.tv__bookmark_set -> selectBookmarkSet()
        }
    }

    private fun selectBookmarkSet() {
        if (mBookmark == null) return
        val args = Bundle()
        val strategy = mBookmarkCategory!!.type
            .filterStrategy
        val snapshot =
            BookmarkManager.INSTANCE
                .getCategoriesSnapshot(strategy)
        val index = snapshot.indexOfOrThrow(mBookmarkCategory!!)
        args.putInt(ChooseBookmarkCategoryFragment.CATEGORY_POSITION, index)
        val className = ChooseBookmarkCategoryFragment::class.java.name
        val frag = Fragment.instantiate(
            activity!!,
            className,
            args
        ) as ChooseBookmarkCategoryFragment
        frag.show(childFragmentManager, null)
    }

    private fun selectBookmarkColor() {
        if (mIcon == null) return
        val args = Bundle()
        args.putInt(BookmarkColorDialogFragment.Companion.ICON_TYPE, mIcon!!.color)
        val dialogFragment =
            Fragment.instantiate(
                activity!!,
                BookmarkColorDialogFragment::class.java.name,
                args
            ) as BookmarkColorDialogFragment
        dialogFragment.setOnColorSetListener(object : OnBookmarkColorChangeListener {
            override fun onBookmarkColorSet(colorPos: Int) {
                val newIcon =
                    BookmarkManager.ICONS[colorPos]
                val from = mIcon!!.name
                val to = newIcon.name
                if (TextUtils.equals(from, to)) return
                Statistics.INSTANCE.trackColorChanged(from, to)
                mIcon = newIcon
                refreshColorMarker()
            }
        })
        dialogFragment.show(activity!!.supportFragmentManager, null)
    }

    private fun refreshColorMarker() {
        if (mIcon != null) {
            val circle =
                Graphics.drawCircleAndImage(
                    mIcon!!.argb(),
                    R.dimen.track_circle_size,
                    R.drawable.ic_bookmark_none,
                    R.dimen.bookmark_icon_size,
                    context!!.resources
                )
            mIvColor!!.setImageDrawable(circle)
        }
    }

    private fun refreshCategory() {
        mTvBookmarkGroup!!.text = mBookmarkCategory!!.name
    }

    private fun refreshBookmark() {
        if (mBookmark == null) return
        if (TextUtils.isEmpty(mEtName!!.text)) mEtName!!.setText(mBookmark!!.name)
        if (TextUtils.isEmpty(mEtDescription!!.text)) {
            mEtDescription!!.setText(
                BookmarkManager.INSTANCE.getBookmarkDescription(mBookmark!!.bookmarkId)
            )
        }
        refreshCategory()
        refreshColorMarker()
    }

    override fun onCategoryChanged(newCategory: BookmarkCategory) {
        mBookmarkCategory = newCategory
        refreshCategory()
    }

    fun setEditBookmarkListener(listener: EditBookmarkListener?) {
        mListener = listener
    }

    companion object {
        const val EXTRA_CATEGORY_ID = "CategoryId"
        const val EXTRA_BOOKMARK_ID = "BookmarkId"
        @kotlin.jvm.JvmStatic
        fun editBookmark(
            categoryId: Long, bookmarkId: Long, context: Context,
            manager: FragmentManager,
            listener: EditBookmarkListener?
        ) {
            val args = Bundle()
            args.putLong(EXTRA_CATEGORY_ID, categoryId)
            args.putLong(EXTRA_BOOKMARK_ID, bookmarkId)
            val name = EditBookmarkFragment::class.java.name
            val fragment = Fragment.instantiate(
                context,
                name,
                args
            ) as EditBookmarkFragment
            fragment.arguments = args
            fragment.setEditBookmarkListener(listener)
            fragment.show(manager, name)
        }
    }
}