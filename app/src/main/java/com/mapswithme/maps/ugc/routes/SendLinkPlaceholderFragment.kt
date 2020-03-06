package com.mapswithme.maps.ugc.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseAuthFragment
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkCategory.AccessRules
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.BookmarksCatalogListener
import com.mapswithme.maps.bookmarks.data.BookmarkManager.UploadResult
import com.mapswithme.maps.bookmarks.data.CatalogCustomProperty
import com.mapswithme.maps.bookmarks.data.CatalogTagsGroup
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.maps.dialog.AlertDialogCallback
import com.mapswithme.util.sharing.TargetUtils
import com.mapswithme.util.statistics.Statistics
import java.util.*

class SendLinkPlaceholderFragment : BaseAuthFragment(), BookmarksCatalogListener,
    AlertDialogCallback {
    private lateinit var mCategory: BookmarkCategory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: throw IllegalArgumentException("Please, setup arguments")
        mCategory =
            Objects.requireNonNull(args.getParcelable(EXTRA_CATEGORY))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_ugc_route_send_link, container, false)
        val closeBtn = root.findViewById<View>(R.id.close_btn)
        val finishClickListener =
            View.OnClickListener { v: View? -> activity!!.finish() }
        closeBtn.setOnClickListener(finishClickListener)
        val cancelBtn = root.findViewById<View>(R.id.cancel_btn)
        cancelBtn.setOnClickListener(finishClickListener)
        val sendMeLinkBtn =
            root.findViewById<View>(R.id.send_me_link_btn)
        sendMeLinkBtn.setOnClickListener { v: View? -> onSendMeLinkBtnClicked() }
        return root
    }

    private fun onSendMeLinkBtnClicked() {
        if (mCategory.accessRules === AccessRules.ACCESS_RULES_LOCAL) requestUpload() else shareLink()
    }

    private fun requestUpload() {
        showProgress()
        BookmarkManager.INSTANCE.uploadToCatalog(
            AccessRules.ACCESS_RULES_AUTHOR_ONLY,
            mCategory
        )
    }

    private fun shareLink() {
        shareLink(
            BookmarkManager.INSTANCE.getWebEditorUrl(
                mCategory.serverId
            ), requireActivity()
        )
        Statistics.INSTANCE.trackEvent(Statistics.EventName.BM_EDIT_ON_WEB_CLICK)
    }

    override fun onUploadFinished(
        uploadResult: UploadResult,
        description: String,
        originCategoryId: Long,
        resultCategoryId: Long
    ) {
        hideProgress()
        if (uploadResult === UploadResult.UPLOAD_RESULT_SUCCESS) onUploadSucceeded() else if (uploadResult === UploadResult.UPLOAD_RESULT_AUTH_ERROR) authorize() else onUploadFailed()
    }

    private fun onUploadFailed() {
        val dialog =
            AlertDialog.Builder()
                .setTitleId(R.string.bookmarks_convert_error_title)
                .setMessageId(R.string.upload_error_toast)
                .setPositiveBtnId(R.string.try_again)
                .setNegativeBtnId(R.string.cancel)
                .setReqCode(REQ_CODE_ERROR_EDITED_ON_WEB_DIALOG)
                .setFragManagerStrategyType(AlertDialog.FragManagerStrategyType.ACTIVITY_FRAGMENT_MANAGER)
                .build()
        dialog.setTargetFragment(
            this,
            REQ_CODE_ERROR_EDITED_ON_WEB_DIALOG
        )
        dialog.show(this, ERROR_EDITED_ON_WEB_DIALOG_REQ_TAG)
    }

    private fun onUploadSucceeded() {
        mCategory = BookmarkManager.INSTANCE.allCategoriesSnapshot.refresh(mCategory)
        shareLink()
    }

    override fun onStart() {
        super.onStart()
        BookmarkManager.INSTANCE.addCatalogListener(this)
    }

    override fun onStop() {
        super.onStop()
        BookmarkManager.INSTANCE.removeCatalogListener(this)
    }

    override fun onImportStarted(serverId: String) { /* do noting by default */
    }

    override fun onImportFinished(
        serverId: String,
        catId: Long,
        successful: Boolean
    ) { /* do noting by default */
    }

    override fun onTagsReceived(
        successful: Boolean, tagsGroups: List<CatalogTagsGroup>,
        tagsLimit: Int
    ) { /* do noting by default */
    }

    override fun onCustomPropertiesReceived(
        successful: Boolean,
        properties: List<CatalogCustomProperty>
    ) { /* do noting by default */
    }

    override fun onUploadStarted(originCategoryId: Long) { /* do noting by default */
    }

    override fun onAuthorizationFinish(success: Boolean) {
        if (success) requestUpload()
    }

    override fun onAuthorizationStart() { /* do noting by default */
    }

    override fun onSocialAuthenticationCancel(type: Int) { /* do noting by default */
    }

    override fun onSocialAuthenticationError(
        type: Int,
        error: String?
    ) { /* do noting by default */
    }

    override fun onAlertDialogPositiveClick(requestCode: Int, which: Int) {
        shareLink()
    }

    override fun onAlertDialogNegativeClick(
        requestCode: Int,
        which: Int
    ) { /* do noting by default */
    }

    override fun onAlertDialogCancel(requestCode: Int) { /* do noting by default */
    }

    companion object {
        const val EXTRA_CATEGORY = "bookmarks_category"
        private const val BODY_STRINGS_SEPARATOR = "\n\n"
        private const val ERROR_EDITED_ON_WEB_DIALOG_REQ_TAG = "error_edited_on_web_dialog"
        private const val REQ_CODE_ERROR_EDITED_ON_WEB_DIALOG = 105
        fun shareLink(url: String, activity: FragmentActivity) {
            val emailBody = (activity.getString(R.string.edit_your_guide_email_body)
                    + BODY_STRINGS_SEPARATOR + url)
            ShareCompat.IntentBuilder.from(activity)
                .setType(TargetUtils.TYPE_TEXT_PLAIN)
                .setSubject(activity.getString(R.string.edit_guide_title))
                .setText(emailBody)
                .setChooserTitle(activity.getString(R.string.share))
                .startChooser()
        }
    }
}