package com.mapswithme.maps.ugc.routes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.*
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.adapter.*
import com.mapswithme.maps.adapter.TagsAdapter.TagViewHolder
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksCatalogListener
import com.mapswithme.maps.bookmarks.data.BookmarkManager.UploadResult
import com.mapswithme.maps.bookmarks.data.CatalogCustomProperty
import com.mapswithme.maps.bookmarks.data.CatalogTag
import com.mapswithme.maps.bookmarks.data.CatalogTagsGroup
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory
import com.mapswithme.util.UiUtils
import java.util.*

class UgcRouteTagsFragment : BaseMwmFragment(), BookmarksCatalogListener,
    OnItemClickListener<Pair<TagsAdapter, TagViewHolder>>,
    AlertDialogCallback {
    private lateinit var mRecycler: RecyclerView
    private lateinit var mProgress: View
    private lateinit var mTagsContainer: ViewGroup
    private var mSavedInstanceState: Bundle? = null
    private var mTagsAdapter: TagsCompositeAdapter? = null
    private lateinit var mDescriptionView: TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_ugc_routes, container, false) as ViewGroup
        setHasOptionsMenu(true)
        mProgress = root.findViewById(R.id.progress_container)
        mTagsContainer = root.findViewById(R.id.tags_container)
        mDescriptionView = root.findViewById(R.id.ugc_route_tags_desc)
        initRecycler(root)
        UiUtils.hide(mTagsContainer)
        UiUtils.show(mProgress)
        BookmarkManager.INSTANCE.requestRouteTags()
        mSavedInstanceState = savedInstanceState
        return root
    }

    private fun initRecycler(root: ViewGroup) {
        mRecycler = root.findViewById(R.id.recycler)
        mRecycler.itemAnimator = null
        val decor = ItemDecoratorFactory.createRatingRecordDecorator(
            context!!.applicationContext,
            DividerItemDecoration.VERTICAL,
            R.drawable.divider_transparent_half_plus_eight
        )
        mRecycler.addItemDecoration(decor)
    }

    private fun onRetryClicked() {
        UiUtils.hide(mTagsContainer)
        UiUtils.show(mProgress)
        BookmarkManager.INSTANCE.requestRouteTags()
    }

    private fun showErrorLoadingDialog() {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(R.string.title_error_downloading_bookmarks)
                .setMessageId(R.string.tags_loading_error_subtitle)
                .setPositiveBtnId(R.string.try_again)
                .setNegativeBtnId(R.string.cancel)
                .setReqCode(ERROR_LOADING_DIALOG_REQ_CODE)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .build()
        dialog.setTargetFragment(this, ERROR_LOADING_DIALOG_REQ_CODE)
        dialog.show(this, ERROR_LOADING_DIALOG_TAG)
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val item = menu.findItem(R.id.done)
        item.isVisible = hasSelectedItems()
    }

    private fun hasSelectedItems(): Boolean {
        return mTagsAdapter != null && mTagsAdapter!!.hasSelectedItems()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.done) {
            onDoneOptionItemClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onDoneOptionItemClicked() {
        if (mTagsAdapter == null) return
        val value =
            ArrayList(mTagsAdapter!!.selectedTags)
        val result =
            Intent().putParcelableArrayListExtra(UgcRouteTagsActivity.Companion.EXTRA_TAGS, value)
        activity!!.setResult(Activity.RESULT_OK, result)
        activity!!.finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mTagsAdapter != null) outState.putParcelableArrayList(
            BUNDLE_SELECTED_TAGS,
            ArrayList(mTagsAdapter!!.selectedTags)
        )
    }

    override fun onStart() {
        super.onStart()
        BookmarkManager.INSTANCE.addCatalogListener(this)
    }

    override fun onStop() {
        super.onStop()
        BookmarkManager.INSTANCE.removeCatalogListener(this)
    }

    override fun onImportStarted(serverId: String) { /* Do nothing by default */
    }

    override fun onImportFinished(
        serverId: String,
        catId: Long,
        successful: Boolean
    ) { /* Do nothing by default */
    }

    override fun onTagsReceived(
        successful: Boolean, tagsGroups: List<CatalogTagsGroup>,
        tagsLimit: Int
    ) {
        UiUtils.showIf(successful && tagsGroups.size != 0, mTagsContainer)
        UiUtils.hide(mProgress)
        if (tagsGroups.size == 0 || !successful) {
            showErrorLoadingDialog()
            return
        }
        installTags(tagsGroups, tagsLimit)
    }

    override fun onCustomPropertiesReceived(
        successful: Boolean,
        properties: List<CatalogCustomProperty>
    ) { /* Not ready yet */
    }

    private fun installTags(
        tagsGroups: List<CatalogTagsGroup>,
        tagsLimit: Int
    ) {
        val savedStateTags =
            validateSavedState(mSavedInstanceState)
        val categoryAdapter = TagGroupNameAdapter(tagsGroups)
        mTagsAdapter = TagsCompositeAdapter(
            context!!, tagsGroups.toMutableList(), savedStateTags.toMutableList(), this,
            tagsLimit
        )
        val compositeAdapter =
            makeCompositeAdapter(categoryAdapter, mTagsAdapter!!)
        val layoutManager =
            LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
            )
        mRecycler.layoutManager = layoutManager
        mRecycler.adapter = compositeAdapter
        val description =
            getString(R.string.ugc_route_tags_desc, tagsLimit.toString())
        mDescriptionView.text = description
        requireActivity().invalidateOptionsMenu()
    }

    override fun onUploadStarted(originCategoryId: Long) { /* Do nothing by default */
    }

    override fun onUploadFinished(
        uploadResult: UploadResult, description: String,
        originCategoryId: Long, resultCategoryId: Long
    ) { /* Do nothing by default */
    }

    override fun onItemClick(v: View, item: Pair<TagsAdapter, TagViewHolder>) {
        ActivityCompat.invalidateOptionsMenu(activity)
        Objects.requireNonNull(mTagsAdapter)
        for (i in 0 until mTagsAdapter!!.itemCount) {
            mTagsAdapter!!.getItem(i).notifyDataSetChanged()
        }
    }

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        onRetryClicked()
    }

    override fun onAlertDialogNegativeClick(requestCode: Int, which: Int) {
        activity!!.setResult(Activity.RESULT_CANCELED)
        activity!!.finish()
    }

    override fun onAlertDialogCancel(requestCode: Int) {
        activity!!.setResult(Activity.RESULT_CANCELED)
        activity!!.finish()
    }

    companion object {
        private const val BUNDLE_SELECTED_TAGS = "bundle_saved_tags"
        private const val ERROR_LOADING_DIALOG_TAG = "error_loading_dialog"
        private const val ERROR_LOADING_DIALOG_REQ_CODE = 205
        private fun validateSavedState(savedState: Bundle?): List<CatalogTag> {
            val tags: List<CatalogTag>? =
                savedState!!.getParcelableArrayList(BUNDLE_SELECTED_TAGS)
            return if (savedState == null || tags != null|| tags!!.isEmpty()) emptyList()
            else tags
        }

        private fun makeCompositeAdapter(
            categoryAdapter: TagGroupNameAdapter,
            tagsCompositeAdapter: TagsCompositeAdapter
        ): RecyclerCompositeAdapter {
            val converter: AdapterPositionConverter = RepeatablePairPositionConverter(
                categoryAdapter,
                tagsCompositeAdapter
            )
            return RecyclerCompositeAdapter(converter, categoryAdapter, tagsCompositeAdapter)
        }
    }
}