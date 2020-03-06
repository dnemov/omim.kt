package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.util.statistics.Statistics

class ShowOnMapCatalogCategoryFragment : DialogFragment() {
    private lateinit var mCategory: BookmarkCategory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        mCategory = getCategoryOrThrow(args)
    }

    private fun getCategoryOrThrow(args: Bundle?): BookmarkCategory {
        val category: BookmarkCategory?  = args?.getParcelable(ARGS_CATEGORY) as BookmarkCategory?
        require(
            category != null
        ) { "Category not found" }
        return category
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_show_on_map_catalog_category, container, false)
        val acceptBtn =
            root.findViewById<View>(R.id.show_on_map_accept_btn)
        acceptBtn.setOnClickListener { v: View? -> onAccepted() }
        root.setOnClickListener { view: View? -> onDeclined() }
        return root
    }

    private fun onDeclined() {
        Statistics.INSTANCE.trackDownloadBookmarkDialog(Statistics.ParamValue.NOT_NOW)
        dismissAllowingStateLoss()
    }

    private fun onAccepted() {
        Statistics.INSTANCE.trackDownloadBookmarkDialog(Statistics.ParamValue.VIEW_ON_MAP)
        val result = Intent().putExtra(
            BookmarksCatalogActivity.EXTRA_DOWNLOADED_CATEGORY,
            mCategory
        )
        activity!!.setResult(Activity.RESULT_OK, result)
        dismissAllowingStateLoss()
        activity!!.finish()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Statistics.INSTANCE.trackDownloadBookmarkDialog(Statistics.ParamValue.CLICK_OUTSIDE)
    }

    fun setCategory(category: BookmarkCategory) {
        mCategory = category
    }

    companion object {
        val TAG = ShowOnMapCatalogCategoryFragment::class.java.canonicalName
        const val ARGS_CATEGORY = "downloaded_category"
        fun newInstance(category: BookmarkCategory): ShowOnMapCatalogCategoryFragment {
            val args = Bundle()
            args.putParcelable(ARGS_CATEGORY, category)
            val fragment = ShowOnMapCatalogCategoryFragment()
            fragment.arguments = args
            return fragment
        }
    }
}