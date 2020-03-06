package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.bookmarks.ChooseBookmarkCategoryAdapter.CategoryListener
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.dialog.EditTextDialogFragment
import com.mapswithme.maps.dialog.EditTextDialogFragment.EditTextDialogInterface
import com.mapswithme.maps.dialog.EditTextDialogFragment.OnTextSaveListener
import com.mapswithme.util.statistics.Statistics
import org.solovyev.android.views.llm.LinearLayoutManager

class ChooseBookmarkCategoryFragment : BaseMwmDialogFragment(),
    EditTextDialogInterface, CategoryListener {
    private var mAdapter: ChooseBookmarkCategoryAdapter? = null
    private var mRecycler: RecyclerView? = null

    interface Listener {
        fun onCategoryChanged(newCategory: BookmarkCategory)
    }

    private var mListener: Listener? =
        null

    override val style: Int
        protected get() = STYLE_NO_TITLE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.choose_bookmark_category_fragment, container, false)
        mRecycler = root.findViewById(R.id.recycler)
        mRecycler?.layoutManager = LinearLayoutManager(activity)
        return root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments
        val catPosition =
            args!!.getInt(CATEGORY_POSITION, 0)
        val items: List<BookmarkCategory> =
            BookmarkManager.INSTANCE.ownedCategoriesSnapshot.items
        mAdapter = ChooseBookmarkCategoryAdapter(activity, catPosition, items)
        mAdapter!!.setListener(this)
        mRecycler!!.adapter = mAdapter
    }

    override fun onAttach(activity: Activity) {
        if (mListener == null) {
            val parent = parentFragment
            if (parent is Listener) mListener =
                parent else if (activity is Listener) mListener =
                activity
        }
        super.onAttach(activity)
    }

    override val saveTextListener: OnTextSaveListener
        get() = object : OnTextSaveListener {
            override fun onSaveText(text: String) {
                createCategory(text)
            }
        }


    override val validator: EditTextDialogFragment.Validator
        get() = CategoryValidator()


    private fun createCategory(name: String) {
        BookmarkManager.INSTANCE.createCategory(name)
        val bookmarkCategories =
            mAdapter!!.bookmarkCategories
        if (bookmarkCategories.size == 0) throw AssertionError("BookmarkCategories are empty")
        val categoryPosition = bookmarkCategories.size - 1
        mAdapter!!.chooseItem(categoryPosition)
        if (mListener != null) {
            val newCategory = bookmarkCategories[categoryPosition]
            mListener!!.onCategoryChanged(newCategory)
        }
        dismiss()
        Statistics.INSTANCE.trackEvent(Statistics.EventName.BM_GROUP_CREATED)
    }

    override fun onCategorySet(categoryPosition: Int) {
        mAdapter!!.chooseItem(categoryPosition)
        if (mListener != null) {
            val category = mAdapter!!.bookmarkCategories[categoryPosition]
            mListener!!.onCategoryChanged(category)
        }
        dismiss()
        Statistics.INSTANCE.trackEvent(Statistics.EventName.BM_GROUP_CHANGED)
    }

    override fun onCategoryCreate() {
        EditTextDialogFragment.show(
            getString(R.string.bookmark_set_name), null,
            getString(R.string.ok), null, this
        )
    }

    companion object {
        const val CATEGORY_POSITION = "ExtraCategoryPosition"
    }
}