package com.mapswithme.maps.ugc.routes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksCatalogListener
import com.mapswithme.maps.bookmarks.data.BookmarkManager.UploadResult
import com.mapswithme.maps.bookmarks.data.CatalogCustomProperty
import com.mapswithme.maps.bookmarks.data.CatalogPropertyOptionAndKey
import com.mapswithme.maps.bookmarks.data.CatalogTagsGroup
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.util.UiUtils
import java.util.*

class UgcRoutePropertiesFragment : BaseMwmFragment(), BookmarksCatalogListener,
    AlertDialogCallback {
    private var mProps: List<CatalogCustomProperty> = emptyList()
    private var mSelectedOption: CatalogPropertyOptionAndKey? = null
    private lateinit var mProgress: View
    private lateinit var mPropsContainer: View
    private lateinit var mLeftBtn: Button
    private lateinit var mRightBtn: Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_ugc_routes_properties, container, false)
        initPropsAndOptions(savedInstanceState)
        initViews(root)
        if (mProps.isEmpty()) BookmarkManager.INSTANCE.requestCustomProperties()
        return root
    }

    private fun initPropsAndOptions(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        mProps = savedInstanceState.getParcelableArrayList(BUNDLE_CUSTOM_PROPS)!!
        mSelectedOption =
            savedInstanceState.getParcelable(BUNDLE_SELECTED_OPTION)
    }

    private fun initViews(root: View) {
        mLeftBtn = root.findViewById(R.id.left_btn)
        mLeftBtn.setOnClickListener { v: View? -> onLeftBtnClicked() }
        mRightBtn = root.findViewById(R.id.right_btn)
        mRightBtn.setOnClickListener { v: View? -> onRightBtnClicked() }
        mPropsContainer = root.findViewById(R.id.properties_container)
        UiUtils.hideIf(mProps.isEmpty(), mPropsContainer)
        mProgress = root.findViewById(R.id.progress)
        UiUtils.showIf(mProps.isEmpty(), mProgress)
        if (mProps.isEmpty()) return
        initButtons()
    }

    private fun initButtons() {
        val property = mProps[0]
        val firstOption =
            property!!.options[FIRST_OPTION_INDEX]
        val secondOption =
            property.options[SECOND_OPTION_INDEX]
        mLeftBtn.text = firstOption.localizedName
        mRightBtn.text = secondOption.localizedName
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mSelectedOption != null) outState.putParcelable(
            BUNDLE_SELECTED_OPTION,
            mSelectedOption
        )
        outState.putParcelableArrayList(
            BUNDLE_CUSTOM_PROPS,
            ArrayList(mProps)
        )
    }

    private fun onBtnClicked(index: Int) {
        val property = mProps[0]
        val option = property!!.options[index]
        mSelectedOption = CatalogPropertyOptionAndKey(property.key, option)
        val intent = Intent(context, UgcRouteTagsActivity::class.java)
        startActivityForResult(intent, REQ_CODE_TAGS_ACTIVITY)
    }

    private fun onRightBtnClicked() {
        onBtnClicked(SECOND_OPTION_INDEX)
    }

    private fun onLeftBtnClicked() {
        onBtnClicked(FIRST_OPTION_INDEX)
    }

    override fun onStart() {
        super.onStart()
        BookmarkManager.INSTANCE.addCatalogListener(this)
    }

    override fun onStop() {
        super.onStop()
        BookmarkManager.INSTANCE.removeCatalogListener(this)
    }

    override fun onImportStarted(serverId: String) { /* Do noting by default */
    }

    override fun onImportFinished(
        serverId: String,
        catId: Long,
        successful: Boolean
    ) { /* Do noting by default */
    }

    override fun onTagsReceived(
        successful: Boolean, tagsGroups: List<CatalogTagsGroup>,
        tagsLimit: Int
    ) { /* Do noting by default */
    }

    override fun onCustomPropertiesReceived(
        successful: Boolean,
        properties: List<CatalogCustomProperty>
    ) {
        if (!successful) {
            onLoadFailed()
            return
        }
        if (properties.isEmpty()) {
            onLoadFailed()
            return
        }
        val property = properties.iterator().next()
        if (property == null) {
            onLoadFailed()
            return
        }
        val options = property.options
        if (options.size <= SECOND_OPTION_INDEX) {
            onLoadFailed()
            return
        }
        onLoadSuccess(properties)
    }

    private fun onLoadFailed() {
        showLoadFailedDialog()
        UiUtils.hide(mProgress, mPropsContainer)
    }

    private fun showLoadFailedDialog() {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(R.string.discovery_button_viator_error_title)
                .setMessageId(R.string.properties_loading_error_subtitle)
                .setPositiveBtnId(R.string.try_again)
                .setNegativeBtnId(R.string.cancel)
                .setReqCode(REQ_CODE_LOAD_FAILED)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .build()
        dialog.setTargetFragment(this, REQ_CODE_LOAD_FAILED)
        dialog.show(this, ERROR_LOADING_DIALOG_TAG)
    }

    private fun onLoadSuccess(properties: List<CatalogCustomProperty>) {
        mProps = properties
        mPropsContainer.visibility = View.VISIBLE
        mProgress.visibility = View.GONE
        initButtons()
    }

    override fun onUploadStarted(originCategoryId: Long) { /* Do noting by default */
    }

    override fun onUploadFinished(
        uploadResult: UploadResult,
        description: String, originCategoryId: Long,
        resultCategoryId: Long
    ) { /* Do noting by default */
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_TAGS_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                val intent = Intent()
                val options =
                    ArrayList(prepareSelectedOptions())
                intent.putParcelableArrayListExtra(
                    EXTRA_CATEGORY_OPTIONS,
                    options
                )
                intent.putExtra(
                    EXTRA_TAGS_ACTIVITY_RESULT,
                    data!!.extras
                )
                activity!!.setResult(Activity.RESULT_OK, intent)
                activity!!.finish()
            }
        }
    }

    private fun prepareSelectedOptions(): List<CatalogPropertyOptionAndKey> {
        return if (mSelectedOption == null) emptyList() else listOf(mSelectedOption!!)
    }

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        UiUtils.show(mProgress)
        UiUtils.hide(mPropsContainer)
        BookmarkManager.INSTANCE.requestCustomProperties()
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
        const val EXTRA_TAGS_ACTIVITY_RESULT = "tags_activity_result"
        const val EXTRA_CATEGORY_OPTIONS = "category_options"
        const val REQ_CODE_TAGS_ACTIVITY = 102
        private const val REQ_CODE_LOAD_FAILED = 101
        private const val FIRST_OPTION_INDEX = 0
        private const val SECOND_OPTION_INDEX = 1
        private const val BUNDLE_SELECTED_OPTION = "selected_property"
        private const val BUNDLE_CUSTOM_PROPS = "custom_props"
        private const val ERROR_LOADING_DIALOG_TAG = "error_loading_dialog"
    }
}