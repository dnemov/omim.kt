package com.mapswithme.maps.ugc.routes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseToolbarAuthFragment
import com.mapswithme.maps.base.FinishActivityToolbarController
import com.mapswithme.maps.bookmarks.data.*
import com.mapswithme.maps.bookmarks.data.BookmarkCategory.AccessRules
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksCatalogListener
import com.mapswithme.maps.bookmarks.data.BookmarkManager.UploadResult
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.maps.dialog.ConfirmationDialogFactory
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.sharing.TargetUtils
import com.mapswithme.util.statistics.Statistics
import java.util.*

class UgcSharingOptionsFragment : BaseToolbarAuthFragment(), BookmarksCatalogListener,
    AlertDialogCallback {
    private lateinit var mGetDirectLinkContainer: View
    private lateinit var mCategory: BookmarkCategory
    private lateinit var mPublishCategoryImage: View
    private lateinit var mGetDirectLinkImage: View
    private lateinit var mEditOnWebBtn: View
    private lateinit var mPublishingCompletedStatusContainer: View
    private lateinit var mGetDirectLinkCompletedStatusContainer: View
    private lateinit var mUploadAndPublishText: TextView
    private lateinit var mGetDirectLinkText: TextView
    private var mCurrentMode: AccessRules? = null
    private lateinit var mUpdateGuideDirectLinkBtn: View
    private lateinit var mUpdateGuidePublicAccessBtn: View
    private lateinit var mDirectLinkCreatedText: TextView
    private lateinit var mDirectLinkDescriptionText: TextView
    private lateinit var mShareDirectLinkBtn: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = Objects.requireNonNull(arguments)
        val rawCategory: BookmarkCategory =
            args!!.getParcelable(BaseUgcRouteActivity.EXTRA_BOOKMARK_CATEGORY)!!
        val snapshot =
            BookmarkManager.INSTANCE.allCategoriesSnapshot
        mCategory = snapshot.refresh(Objects.requireNonNull(rawCategory))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_ugc_routes_sharing_options, container, false)
        initViews(root)
        initClickListeners(root)
        mCurrentMode = getCurrentMode(savedInstanceState)
        toggleViews()
        return root
    }

    private fun initViews(root: View) {
        mGetDirectLinkContainer =
            root.findViewById(R.id.get_direct_link_container)
        val publishCategoryContainer =
            root.findViewById<View>(R.id.upload_and_publish_container)
        mPublishCategoryImage =
            publishCategoryContainer.findViewById(R.id.upload_and_publish_image)
        mGetDirectLinkImage =
            mGetDirectLinkContainer.findViewById(R.id.get_direct_link_image)
        mEditOnWebBtn = root.findViewById(R.id.edit_on_web_btn)
        mPublishingCompletedStatusContainer =
            root.findViewById(R.id.publishing_completed_status_container)
        mGetDirectLinkCompletedStatusContainer =
            root.findViewById(R.id.get_direct_link_completed_status_container)
        mUploadAndPublishText = root.findViewById(R.id.upload_and_publish_text)
        mGetDirectLinkText = root.findViewById(R.id.get_direct_link_text)
        mUpdateGuideDirectLinkBtn =
            root.findViewById(R.id.direct_link_update_btn)
        mUpdateGuidePublicAccessBtn =
            root.findViewById(R.id.upload_and_publish_update_btn)
        mDirectLinkCreatedText = root.findViewById(R.id.direct_link_created_text)
        mDirectLinkDescriptionText = root.findViewById(R.id.direct_link_description_text)
        mShareDirectLinkBtn =
            mGetDirectLinkCompletedStatusContainer.findViewById(R.id.share_direct_link_btn)
        mUpdateGuideDirectLinkBtn.isSelected = true
        mUpdateGuidePublicAccessBtn.isSelected = true
        val licenseAgreementText =
            root.findViewById<TextView>(R.id.license_agreement_message)
        val src = resources.getString(
            R.string.ugc_routes_user_agreement,
            Framework.nativeGetPrivacyPolicyLink()
        )
        val spanned = Html.fromHtml(src)
        licenseAgreementText.movementMethod = LinkMovementMethod.getInstance()
        licenseAgreementText.text = spanned
    }

    private fun toggleViews() {
        val isPublished =
            mCategory.accessRules === AccessRules.ACCESS_RULES_PUBLIC
        UiUtils.hideIf(isPublished, mUploadAndPublishText)
        UiUtils.showIf(
            isPublished,
            mPublishingCompletedStatusContainer,
            mUpdateGuidePublicAccessBtn
        )
        mPublishCategoryImage.isSelected = !isPublished
        val isLinkSuccessFormed =
            mCategory.accessRules === AccessRules.ACCESS_RULES_DIRECT_LINK
        UiUtils.hideIf(
            isLinkSuccessFormed
                    || isPublished, mGetDirectLinkText
        )
        UiUtils.showIf(
            isLinkSuccessFormed
                    || isPublished, mGetDirectLinkCompletedStatusContainer
        )
        UiUtils.showIf(isLinkSuccessFormed, mUpdateGuideDirectLinkBtn)
        mGetDirectLinkImage.isSelected = !isLinkSuccessFormed && !isPublished
        mGetDirectLinkCompletedStatusContainer.isEnabled = !isPublished
        mDirectLinkCreatedText.setText(if (isPublished) R.string.upload_and_publish_success else R.string.direct_link_success)
        mDirectLinkDescriptionText.setText(if (isPublished) R.string.unable_get_direct_link_desc else R.string.get_direct_link_desc)
        UiUtils.hideIf(isPublished, mShareDirectLinkBtn)
    }

    override fun onCreateToolbarController(root: View): ToolbarController {
        return FinishActivityToolbarController(root, requireActivity())
    }

    private fun initClickListeners(root: View) {
        val getDirectLinkView =
            root.findViewById<View>(R.id.get_direct_link_text)
        getDirectLinkView.setOnClickListener { directLinkListener: View? -> onGetDirectLinkClicked() }
        mUpdateGuideDirectLinkBtn.setOnClickListener { v: View? -> onUpdateDirectLinkClicked() }
        val uploadAndPublishView =
            root.findViewById<View>(R.id.upload_and_publish_text)
        uploadAndPublishView.setOnClickListener { uploadListener: View? -> onUploadAndPublishBtnClicked() }
        mUpdateGuidePublicAccessBtn.setOnClickListener { v: View? -> onUpdatePublicAccessClicked() }
        mShareDirectLinkBtn.setOnClickListener { v: View? -> onDirectLinkShared() }
        val sharePublishedBtn =
            mPublishingCompletedStatusContainer.findViewById<View>(R.id.share_published_category_btn)
        sharePublishedBtn.setOnClickListener { v: View? -> onPublishedCategoryShared() }
        val editOnWebBtn =
            root.findViewById<View>(R.id.edit_on_web_btn)
        editOnWebBtn.setOnClickListener { v: View? -> onEditOnWebClicked() }
    }

    private fun onUpdatePublicAccessClicked() {
        mCurrentMode = AccessRules.ACCESS_RULES_PUBLIC
        onUpdateClickedInternal()
    }

    private fun onUpdateDirectLinkClicked() {
        mCurrentMode = AccessRules.ACCESS_RULES_DIRECT_LINK
        onUpdateClickedInternal()
    }

    private fun onUpdateClickedInternal() {
        showUpdateCategoryConfirmationDialog()
    }

    private fun onEditOnWebClicked() {
        if (isNetworkConnectionAbsent) {
            showNoNetworkConnectionDialog()
            return
        }
        val intent = Intent(context, SendLinkPlaceholderActivity::class.java)
            .putExtra(SendLinkPlaceholderFragment.Companion.EXTRA_CATEGORY, mCategory)
        startActivity(intent)
        Statistics.INSTANCE.trackSharingOptionsClick(Statistics.ParamValue.EDIT_ON_WEB)
    }

    private fun onPublishedCategoryShared() {
        shareCategory(BookmarkManager.INSTANCE.getCatalogPublicLink(mCategory.id))
    }

    private fun shareCategory(link: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType(TargetUtils.TYPE_TEXT_PLAIN)
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_bookmarks_email_body_link, link))
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
        Statistics.INSTANCE.trackSharingOptionsClick(Statistics.ParamValue.COPY_LINK)
    }

    private fun onDirectLinkShared() {
        shareCategory(BookmarkManager.INSTANCE.getCatalogDeeplink(mCategory.id))
    }

    private fun showNoNetworkConnectionDialog() {
        val fragment =
            requireFragmentManager().findFragmentByTag(NO_NETWORK_CONNECTION_DIALOG_TAG)
        if (fragment != null) return
        val dialog =
            AlertDialog.Builder()
                .setTitleId(R.string.common_check_internet_connection_dialog_title)
                .setMessageId(R.string.common_check_internet_connection_dialog)
                .setPositiveBtnId(R.string.try_again)
                .setNegativeBtnId(R.string.cancel)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .setReqCode(REQ_CODE_NO_NETWORK_CONNECTION_DIALOG)
                .build()
        dialog.setTargetFragment(
            this,
            REQ_CODE_NO_NETWORK_CONNECTION_DIALOG
        )
        dialog.show(this, NO_NETWORK_CONNECTION_DIALOG_TAG)
        Statistics.INSTANCE.trackSharingOptionsError(
            Statistics.EventName.BM_SHARING_OPTIONS_ERROR,
            Statistics.NetworkErrorType.NO_NETWORK
        )
    }

    private val isNetworkConnectionAbsent: Boolean
        private get() = !ConnectionState.isConnected

    private fun openTagsScreen() {
        val intent = Intent(context, EditCategoryNameActivity::class.java)
        intent.putExtra(
            BaseEditUserBookmarkCategoryFragment.Companion.BUNDLE_BOOKMARK_CATEGORY,
            mCategory
        )
        startActivityForResult(
            intent,
            REQ_CODE_CUSTOM_PROPERTIES
        )
    }

    private fun onUploadAndPublishBtnClicked() {
        if (mCategory.size() < MIN_REQUIRED_CATEGORY_SIZE) {
            showNotEnoughBookmarksDialog()
            return
        }
        mCurrentMode = AccessRules.ACCESS_RULES_PUBLIC
        onUploadBtnClicked()
        Statistics.INSTANCE.trackSharingOptionsClick(Statistics.ParamValue.PUBLIC)
    }

    private fun onGetDirectLinkClicked() {
        if (isNetworkConnectionAbsent) {
            showNoNetworkConnectionDialog()
            return
        }
        mCurrentMode = AccessRules.ACCESS_RULES_DIRECT_LINK
        requestUpload()
        Statistics.INSTANCE.trackSharingOptionsClick(Statistics.ParamValue.PRIVATE)
    }

    private fun onUploadBtnClicked() {
        if (isNetworkConnectionAbsent) {
            showNoNetworkConnectionDialog()
            return
        }
        showUploadCatalogConfirmationDialog()
    }

    private fun requestUpload() {
        if (isAuthorized) onPostAuthCompleted() else authorize()
    }

    private fun onPostAuthCompleted() {
        if (isDirectLinkUploadMode) requestDirectLink() else if (isPublishRefreshManual) requestPublishingImmediately() else openTagsScreen()
    }

    private val isPublishRefreshManual: Boolean
        private get() = mCategory.accessRules === AccessRules.ACCESS_RULES_PUBLIC

    private fun requestPublishingImmediately() {
        showProgress()
        BookmarkManager.INSTANCE.uploadToCatalog(
            AccessRules.ACCESS_RULES_PUBLIC,
            mCategory
        )
    }

    private val isDirectLinkUploadMode: Boolean
        private get() = mCurrentMode === AccessRules.ACCESS_RULES_DIRECT_LINK

    private fun requestDirectLink() {
        checkNotNull(mCurrentMode) { "CurrentMode must be initialized" }
        showProgress()
        BookmarkManager.INSTANCE.uploadRoutes(mCurrentMode!!.ordinal, mCategory)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mCurrentMode != null) outState.putInt(
            BUNDLE_CURRENT_MODE,
            mCurrentMode!!.ordinal
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_CUSTOM_PROPERTIES && resultCode == Activity.RESULT_OK) requestPublishing(
            data!!
        )
    }

    private fun requestPublishing(data: Intent) {
        showProgress()
        val tagsActivityResult =
            data.getParcelableExtra<Bundle>(UgcRoutePropertiesFragment.Companion.EXTRA_TAGS_ACTIVITY_RESULT)
        val tags: List<CatalogTag> =
            tagsActivityResult.getParcelableArrayList(UgcRouteTagsActivity.Companion.EXTRA_TAGS)!!
        val options: List<CatalogPropertyOptionAndKey> =
            data.getParcelableArrayListExtra(UgcRoutePropertiesFragment.Companion.EXTRA_CATEGORY_OPTIONS)
        BookmarkManager.INSTANCE.setCategoryTags(mCategory, Objects.requireNonNull(tags))
        BookmarkManager.INSTANCE.setCategoryProperties(mCategory, options)
        BookmarkManager.INSTANCE.uploadToCatalog(
            AccessRules.ACCESS_RULES_PUBLIC,
            mCategory
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

    override fun onAuthorizationFinish(success: Boolean) {
        if (success) onPostAuthCompleted() else onPostAuthFailed()
    }

    private fun onPostAuthFailed() {
        Statistics.INSTANCE.trackSharingOptionsError(
            Statistics.EventName.BM_SHARING_OPTIONS_ERROR,
            Statistics.NetworkErrorType.AUTH_FAILED
        )
    }

    override fun onAuthorizationStart() {}
    override fun onSocialAuthenticationCancel(type: Int) {}
    override fun onSocialAuthenticationError(
        type: Int,
        error: String?
    ) {
    }

    override fun onImportStarted(serverId: String) {}
    override fun onImportFinished(
        serverId: String,
        catId: Long,
        successful: Boolean
    ) {
    }

    override fun onTagsReceived(
        successful: Boolean, tagsGroups: List<CatalogTagsGroup>,
        tagsLimit: Int
    ) {
    }

    override fun onCustomPropertiesReceived(
        successful: Boolean,
        properties: List<CatalogCustomProperty>
    ) {
    }

    override fun onUploadStarted(originCategoryId: Long) {}
    override fun onUploadFinished(
        uploadResult: UploadResult,
        description: String, originCategoryId: Long,
        resultCategoryId: Long
    ) {
        hideProgress()
        if (isOkResult(uploadResult)) onUploadSuccess() else onUploadError(uploadResult)
    }

    private fun onUploadError(uploadResult: UploadResult) {
        Statistics.INSTANCE.trackSharingOptionsError(
            Statistics.EventName.BM_SHARING_OPTIONS_UPLOAD_ERROR,
            uploadResult.ordinal
        )
        if (uploadResult === UploadResult.UPLOAD_RESULT_MALFORMED_DATA_ERROR) {
            showHtmlFormattingError()
            return
        }
        if (uploadResult === UploadResult.UPLOAD_RESULT_ACCESS_ERROR) {
            showUnresolvedConflictsErrorDialog()
            return
        }
        showCommonErrorDialog()
    }

    private fun showCommonErrorDialog() {
        showUploadErrorDialog(
            R.string.upload_error_toast,
            REQ_CODE_ERROR_COMMON,
            ERROR_COMMON_DIALOG_TAG
        )
    }

    private fun onUploadSuccess() {
        val isRefreshManual =
            (mCategory.accessRules === AccessRules.ACCESS_RULES_PUBLIC
                    || mCategory.accessRules === AccessRules.ACCESS_RULES_DIRECT_LINK)
        mCategory = BookmarkManager.INSTANCE.allCategoriesSnapshot.refresh(mCategory)
        checkSuccessUploadedCategoryAccessRules()
        val isDirectLinkMode =
            mCategory.accessRules === AccessRules.ACCESS_RULES_DIRECT_LINK
        val successMsgResId =
            if (isRefreshManual) R.string.direct_link_updating_success else if (isDirectLinkMode) R.string.direct_link_success else R.string.upload_and_publish_success
        Toast.makeText(context, successMsgResId, Toast.LENGTH_SHORT).show()
        toggleViews()
        Statistics.INSTANCE.trackSharingOptionsUploadSuccess(
            mCategory
        )
    }

    private fun checkSuccessUploadedCategoryAccessRules() {
        check(
            !(mCategory.accessRules !== AccessRules.ACCESS_RULES_PUBLIC
                    && mCategory.accessRules !== AccessRules.ACCESS_RULES_DIRECT_LINK)
        ) {
            "Access rules must be ACCESS_RULES_PUBLIC or ACCESS_RULES_DIRECT_LINK." +
                    " Current value = " + mCategory.accessRules
        }
    }

    private fun showUploadErrorDialog(
        @StringRes subtitle: Int, reqCode: Int,
        tag: String
    ) {
        showErrorDialog(R.string.unable_upadate_error_title, subtitle, reqCode, tag)
    }

    private fun showNotEnoughBookmarksDialog() {
        showErrorDialog(
            R.string.error_public_not_enought_title,
            R.string.error_public_not_enought_subtitle,
            REQ_CODE_ERROR_NOT_ENOUGH_BOOKMARKS,
            NOT_ENOUGH_BOOKMARKS_DIALOG_TAG
        )
    }

    private fun showErrorDialog(
        @StringRes title: Int, @StringRes subtitle: Int, reqCode: Int,
        tag: String
    ) {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(title)
                .setMessageId(subtitle)
                .setPositiveBtnId(R.string.ok)
                .setReqCode(reqCode)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .build()
        dialog.setTargetFragment(this, reqCode)
        dialog.show(this, tag)
    }

    private fun isOkResult(uploadResult: UploadResult): Boolean {
        return uploadResult === UploadResult.UPLOAD_RESULT_SUCCESS
    }

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        if (requestCode == REQ_CODE_NO_NETWORK_CONNECTION_DIALOG) Utils.showSystemSettings(
            requireContext()
        ) else if (requestCode == REQ_CODE_UPLOAD_CONFIRMATION_DIALOG
            || requestCode == REQ_CODE_UPDATE_CONFIRMATION_DIALOG
        ) requestUpload() else if (requestCode == REQ_CODE_ERROR_HTML_FORMATTING_DIALOG) SendLinkPlaceholderFragment.Companion.shareLink(
            BookmarkManager.INSTANCE.getWebEditorUrl(mCategory.serverId),
            requireActivity()
        )
    }

    override fun onAlertDialogNegativeClick(requestCode: Int, which: Int) {}
    override fun onAlertDialogCancel(requestCode: Int) {}
    private fun showConfirmationDialog(
        @StringRes title: Int, @StringRes description: Int,
        @StringRes acceptBtn: Int,
        @StringRes declineBtn: Int,
        tag: String, reqCode: Int
    ) {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(title)
                .setMessageId(description)
                .setPositiveBtnId(acceptBtn)
                .setNegativeBtnId(declineBtn)
                .setReqCode(reqCode)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .setDialogViewStrategyType(AlertDialog.DialogViewStrategyType.CONFIRMATION_DIALOG)
                .setDialogFactory(ConfirmationDialogFactory())
                .build()
        dialog.setTargetFragment(this, reqCode)
        dialog.show(this, tag)
    }

    private fun showHtmlFormattingError() {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(R.string.html_format_error_title)
                .setMessageId(R.string.html_format_error_subtitle)
                .setPositiveBtnId(R.string.edit_on_web)
                .setNegativeBtnId(R.string.cancel)
                .setReqCode(REQ_CODE_ERROR_HTML_FORMATTING_DIALOG)
                .setImageResId(R.drawable.ic_error_red)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .setDialogViewStrategyType(AlertDialog.DialogViewStrategyType.CONFIRMATION_DIALOG)
                .setDialogFactory(ConfirmationDialogFactory())
                .build()
        dialog.setTargetFragment(
            this,
            REQ_CODE_ERROR_HTML_FORMATTING_DIALOG
        )
        dialog.show(this, ERROR_HTML_FORMATTING_DIALOG_TAG)
    }

    private fun showUploadCatalogConfirmationDialog() {
        showConfirmationDialog(
            R.string.bookmark_public_upload_alert_title,
            R.string.bookmark_public_upload_alert_subtitle,
            R.string.bookmark_public_upload_alert_ok_button,
            R.string.cancel,
            UPLOAD_CONFIRMATION_DIALOG_TAG,
            REQ_CODE_UPLOAD_CONFIRMATION_DIALOG
        )
    }

    private fun showUpdateCategoryConfirmationDialog() {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(R.string.any_access_update_alert_title)
                .setMessageId(R.string.any_access_update_alert_message)
                .setPositiveBtnId(R.string.any_access_update_alert_update)
                .setNegativeBtnId(R.string.cancel)
                .setReqCode(REQ_CODE_UPDATE_CONFIRMATION_DIALOG)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .build()
        dialog.setTargetFragment(
            this,
            REQ_CODE_UPDATE_CONFIRMATION_DIALOG
        )
        dialog.show(this, UPDATE_CONFIRMATION_DIALOG_TAG)
    }

    private fun showUnresolvedConflictsErrorDialog() {
        showConfirmationDialog(
            R.string.public_or_limited_access_after_edit_online_error_title,
            R.string.public_or_limited_access_after_edit_online_error_message,
            R.string.edit_on_web,
            R.string.cancel,
            UPLOAD_CONFIRMATION_DIALOG_TAG,
            REQ_CODE_UPLOAD_CONFIRMATION_DIALOG
        )
    }

    companion object {
        const val REQ_CODE_CUSTOM_PROPERTIES = 101
        private const val REQ_CODE_NO_NETWORK_CONNECTION_DIALOG = 103
        private const val REQ_CODE_ERROR_COMMON = 106
        private const val REQ_CODE_ERROR_NOT_ENOUGH_BOOKMARKS = 107
        private const val REQ_CODE_UPLOAD_CONFIRMATION_DIALOG = 108
        private const val REQ_CODE_ERROR_HTML_FORMATTING_DIALOG = 109
        private const val REQ_CODE_UPDATE_CONFIRMATION_DIALOG = 110
        private const val BUNDLE_CURRENT_MODE = "current_mode"
        private const val NO_NETWORK_CONNECTION_DIALOG_TAG = "no_network_connection_dialog"
        private const val NOT_ENOUGH_BOOKMARKS_DIALOG_TAG = "not_enough_bookmarks_dialog"
        private const val ERROR_COMMON_DIALOG_TAG = "error_common_dialog"
        private const val UPLOAD_CONFIRMATION_DIALOG_TAG = "upload_confirmation_dialog"
        private const val UPDATE_CONFIRMATION_DIALOG_TAG = "update_confirmation_dialog"
        private const val ERROR_HTML_FORMATTING_DIALOG_TAG = "error_html_formatting_dialog"
        private const val MIN_REQUIRED_CATEGORY_SIZE = 3
        private fun getCurrentMode(bundle: Bundle?): AccessRules? {
            return if (bundle != null && bundle.containsKey(BUNDLE_CURRENT_MODE)) AccessRules.values()[bundle.getInt(
                BUNDLE_CURRENT_MODE
            )] else null
        }
    }
}