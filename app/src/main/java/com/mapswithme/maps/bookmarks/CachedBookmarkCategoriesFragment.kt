package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.cocosw.bottomsheet.BottomSheet
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksCatalogListener
import com.mapswithme.maps.bookmarks.data.BookmarkManager.UploadResult
import com.mapswithme.maps.bookmarks.data.CatalogCustomProperty
import com.mapswithme.maps.bookmarks.data.CatalogTagsGroup
import com.mapswithme.util.SharedPropertiesUtils
import com.mapswithme.util.UTM
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics

class CachedBookmarkCategoriesFragment : BaseBookmarkCategoriesFragment() {
    private lateinit var mEmptyViewContainer: ViewGroup
    private lateinit var mPayloadContainer: View
    private lateinit var mProgressContainer: View
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        mProgressContainer = view.findViewById(R.id.placeholder_loading)
        mEmptyViewContainer = view.findViewById(R.id.placeholder_container)
        mPayloadContainer =
            view.findViewById(R.id.cached_bookmarks_payload_container)
        initHeader(view)
        initPlaceholder()
    }

    private fun initPlaceholder() {
        val downloadRoutesPlaceHolderBtn =
            mEmptyViewContainer.findViewById<View>(R.id.download_routers_btn)
        downloadRoutesPlaceHolderBtn.setOnClickListener(DownloadRoutesClickListener())
    }

    private fun initHeader(view: View) {
        val downloadRoutesBtn =
            view.findViewById<View>(R.id.download_routes_layout)
        downloadRoutesBtn.setOnClickListener { btn: View? -> openBookmarksCatalogScreen() }
        val closeHeaderBtn =
            view.findViewById<View>(R.id.header_close)
        closeHeaderBtn.setOnClickListener(CloseHeaderClickListener())
        val isClosed =
            SharedPropertiesUtils.isCatalogCategoriesHeaderClosed(requireContext())
        val header = view.findViewById<View>(R.id.header)
        UiUtils.hideIf(isClosed, header)
        val imageView =
            downloadRoutesBtn.findViewById<ImageView>(R.id.image)
        val resProvider = adapter!!.factory.resProvider
        imageView.setImageResource(resProvider.footerImage)
        val textView = downloadRoutesBtn.findViewById<TextView>(R.id.text)
        textView.setText(resProvider.footerText)
    }

    override val layoutRes: Int
        protected get() = R.layout.fragment_catalog_bookmark_categories

    override val type: BookmarkCategory.Type
        protected get() = BookmarkCategory.Type.DOWNLOADED

    override fun onItemClick(v: View, category: BookmarkCategory) {
        if (BookmarkManager.INSTANCE.isGuide(category)) Statistics.INSTANCE.trackGuideOpen(
            category.serverId
        )
        super.onItemClick(v, category)
    }

    override fun onFooterClick() {
        openBookmarksCatalogScreen()
    }

    override fun onShareActionSelected(category: BookmarkCategory) {
        throw AssertionError("Sharing is not supported for downloaded guides")
    }

    override fun onDeleteActionSelected() {
        updateLoadingPlaceholder()
    }

    override fun updateLoadingPlaceholder() {
        super.updateLoadingPlaceholder()
        val showLoadingPlaceholder =
            BookmarkManager.INSTANCE.isAsyncBookmarksLoadingInProgress
        if (showLoadingPlaceholder) {
            mProgressContainer.visibility = View.VISIBLE
            mPayloadContainer.visibility = View.GONE
            mEmptyViewContainer.visibility = View.GONE
        } else {
            val isEmptyAdapter = adapter!!.itemCount == 0
            UiUtils.showIf(isEmptyAdapter, mEmptyViewContainer)
            mPayloadContainer.visibility = if (isEmptyAdapter) View.GONE else View.VISIBLE
            mProgressContainer.visibility = View.GONE
            if (!isEmptyAdapter) Statistics.INSTANCE.trackGuidesShown(
                BookmarkManager.INSTANCE.guidesIds
            )
        }
    }

    override val categoryMenuResId: Int
        protected get() = R.menu.menu_catalog_bookmark_categories

    private fun openBookmarksCatalogScreen() {
        val catalogUrl = BookmarkManager.INSTANCE.getCatalogFrontendUrl(
            UTM.UTM_BOOKMARKS_PAGE_CATALOG_BUTTON
        )
        BookmarksCatalogActivity.Companion.startForResult(
            this, REQ_CODE_CATALOG,
            catalogUrl
        )
        Statistics.INSTANCE.trackOpenCatalogScreen()
    }

    public override fun onActivityResultInternal(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == REQ_CODE_CATALOG && resultCode == Activity.RESULT_OK) {
            requireActivity().setResult(Activity.RESULT_OK, data)
            requireActivity().finish()
        }
    }

    override fun onMoreOperationClick(item: BookmarkCategory) {
        showBottomMenu(item)
    }

    override fun createCatalogListener(): BookmarksCatalogListener {
        return BookmarkCategoriesCatalogListener()
    }

    override fun prepareBottomMenuItems(bottomSheet: BottomSheet) {
        setEnableForMenuItem(
            R.id.delete,
            bottomSheet,
            true
        )
        setEnableForMenuItem(
            R.id.share,
            bottomSheet,
            false
        )
    }

    private inner class CloseHeaderClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            val header =
                mPayloadContainer.findViewById<View>(R.id.header)
            header.visibility = View.GONE
            SharedPropertiesUtils.setCatalogCategoriesHeaderClosed(requireContext(), true)
        }
    }

    private inner class DownloadRoutesClickListener :
        View.OnClickListener {
        override fun onClick(v: View) {
            openBookmarksCatalogScreen()
        }
    }

    private inner class BookmarkCategoriesCatalogListener : BookmarksCatalogListener {
        override fun onImportStarted(serverId: String) {
            UiUtils.show(mProgressContainer)
            UiUtils.hide(mEmptyViewContainer, mPayloadContainer)
        }

        override fun onImportFinished(
            serverId: String,
            catId: Long,
            successful: Boolean
        ) {
            if (successful) {
                UiUtils.show(mPayloadContainer)
                UiUtils.hide(mProgressContainer, mEmptyViewContainer)
                adapter!!.notifyDataSetChanged()
                Statistics.INSTANCE.trackGuidesShown(BookmarkManager.INSTANCE.guidesIds)
            } else {
                val isEmptyAdapter = adapter!!.itemCount == 0
                UiUtils.hide(mProgressContainer)
                UiUtils.showIf(isEmptyAdapter, mEmptyViewContainer)
                UiUtils.hideIf(isEmptyAdapter, mPayloadContainer)
            }
        }

        override fun onTagsReceived(
            successful: Boolean, tagsGroups: List<CatalogTagsGroup>,
            tagsLimit: Int
        ) { //TODO(@alexzatsepin): Implement me if necessary
        }

        override fun onCustomPropertiesReceived(
            successful: Boolean,
            properties: List<CatalogCustomProperty>
        ) { //TODO(@alexzatsepin): Implement me if necessary
        }

        override fun onUploadStarted(originCategoryId: Long) { //TODO(@alexzatsepin): Implement me if necessary
        }

        override fun onUploadFinished(
            uploadResult: UploadResult,
            description: String, originCategoryId: Long,
            resultCategoryId: Long
        ) { //TODO(@alexzatsepin): Implement me if necessary
        }
    }
}