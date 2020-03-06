package com.mapswithme.maps.ugc.routes

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.EditText
import android.widget.TextView
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.util.statistics.Statistics
import java.util.*

class UgcRouteEditSettingsFragment : BaseMwmToolbarFragment() {
    private lateinit var mCategory: BookmarkCategory
    private lateinit var mAccessRulesView: TextView
    private lateinit var mEditDescView: EditText
    private lateinit var mEditCategoryNameView: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: throw IllegalArgumentException("Args must be not null")
        mCategory =
            Objects.requireNonNull(args.getParcelable(BaseUgcRouteActivity.Companion.EXTRA_BOOKMARK_CATEGORY))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_ugc_route_edit, container, false)
        setHasOptionsMenu(true)
        initViews(root)
        return root
    }

    private fun initViews(root: View) {
        mEditCategoryNameView = root.findViewById(R.id.edit_category_name_view)
        mEditCategoryNameView.setText(mCategory.name)
        mEditCategoryNameView.requestFocus()
        mAccessRulesView = root.findViewById(R.id.sharing_options_desc)
        mAccessRulesView.setText(mCategory.accessRules.nameResId)
        mEditDescView = root.findViewById(R.id.edit_description)
        mEditDescView.setText(mCategory.description)
        val clearNameBtn =
            root.findViewById<View>(R.id.edit_text_clear_btn)
        clearNameBtn.setOnClickListener { v: View? ->
            mEditCategoryNameView.editableText.clear()
        }
        val sharingOptionsBtn =
            root.findViewById<View>(R.id.open_sharing_options_screen_btn_container)
        sharingOptionsBtn.setOnClickListener { v: View? -> onSharingOptionsClicked() }
        sharingOptionsBtn.isEnabled = mCategory.isSharingOptionsAllowed
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        mCategory = BookmarkManager.INSTANCE.allCategoriesSnapshot.refresh(mCategory)
        mAccessRulesView.setText(mCategory.accessRules.nameResId)
    }

    private fun onSharingOptionsClicked() {
        openSharingOptionsScreen()
        Statistics.INSTANCE.trackEditSettingsSharingOptionsClick()
    }

    private fun openSharingOptionsScreen() {
        UgcRouteSharingOptionsActivity.Companion.startForResult(activity!!, mCategory)
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.done) {
            onEditDoneClicked()
            activity!!.finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed(): Boolean {
        if (isCategoryDescChanged) Statistics.INSTANCE.trackCategoryDescChanged()
        Statistics.INSTANCE.trackEditSettingsCancel()
        return super.onBackPressed()
    }

    private fun onEditDoneClicked() {
        if (isCategoryNameChanged) BookmarkManager.INSTANCE.setCategoryName(
            mCategory.id,
            editableCategoryName
        )
        if (isCategoryDescChanged) {
            BookmarkManager.INSTANCE.setCategoryDescription(mCategory.id, editableCategoryDesc)
            Statistics.INSTANCE.trackCategoryDescChanged()
        }
        Statistics.INSTANCE.trackEditSettingsConfirm()
    }

    private val isCategoryNameChanged: Boolean
        private get() {
            val categoryName = editableCategoryName
            return !TextUtils.equals(categoryName, mCategory.name)
        }

    private val editableCategoryName: String
        private get() = mEditCategoryNameView.editableText.toString().trim { it <= ' ' }

    private val editableCategoryDesc: String
        private get() = mEditDescView.editableText.toString().trim { it <= ' ' }

    private val isCategoryDescChanged: Boolean
        private get() {
            val categoryDesc = editableCategoryDesc
            return !TextUtils.equals(mCategory.description, categoryDesc)
        }
}