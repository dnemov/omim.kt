package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import com.cocosw.bottomsheet.BottomSheet
import com.mapswithme.maps.R
import com.mapswithme.maps.adapter.OnItemClickListener
import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.base.DataChangedListener
import com.mapswithme.maps.bookmarks.BaseBookmarkCategoriesFragment.MenuClickProcessorBase.*
import com.mapswithme.maps.bookmarks.KmlImportController.ImportKmlCallback
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import com.mapswithme.maps.bookmarks.data.BookmarkManager.*
import com.mapswithme.maps.bookmarks.data.BookmarkSharingResult
import com.mapswithme.maps.dialog.EditTextDialogFragment
import com.mapswithme.maps.dialog.EditTextDialogFragment.EditTextDialogInterface
import com.mapswithme.maps.dialog.EditTextDialogFragment.OnTextSaveListener
import com.mapswithme.maps.ugc.routes.UgcRouteEditSettingsActivity
import com.mapswithme.maps.ugc.routes.UgcRouteSharingOptionsActivity
import com.mapswithme.maps.widget.PlaceholderView
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory
import com.mapswithme.util.BottomSheetHelper
import com.mapswithme.util.UiUtils
import com.mapswithme.util.sharing.SharingHelper
import com.mapswithme.util.statistics.Analytics
import com.mapswithme.util.statistics.Statistics

abstract class BaseBookmarkCategoriesFragment :
    BaseMwmRecyclerFragment<BookmarkCategoriesAdapter>(), EditTextDialogInterface,
    MenuItem.OnMenuItemClickListener, BookmarksLoadingListener,
    BookmarksSharingListener, CategoryListCallback, ImportKmlCallback,
    OnItemClickListener<BookmarkCategory>,
    OnItemLongClickListener<BookmarkCategory> {
    private var mSelectedCategory: BookmarkCategory? = null
    private var mCategoryEditor: CategoryEditor? = null
    private var mKmlImportController: KmlImportController? = null
    private val mImportKmlTask: Runnable = ImportKmlTask()
    private lateinit var mCatalogListener: BookmarksCatalogListener
    private lateinit var mCategoriesAdapterObserver: DataChangedListener<*>
    @get:LayoutRes
    override val layoutRes: Int
        protected get() = R.layout.fragment_bookmark_categories

    override fun createAdapter(): BookmarkCategoriesAdapter {
        val strategy = type.filterStrategy
        val items =
            INSTANCE.getCategoriesSnapshot(strategy)
                .items
        return BookmarkCategoriesAdapter(requireContext(), type, items)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        onPrepareControllers(view)
        adapter?.setOnClickListener(this)
        adapter?.setOnLongClickListener(this)
        adapter?.setCategoryListCallback(this)
        val rw = recyclerView ?: return
        rw.isNestedScrollingEnabled = false
        val decor = ItemDecoratorFactory
            .createVerticalDefaultDecorator(context!!)
        rw.addItemDecoration(decor)
        mCatalogListener = CatalogListenerDecorator(createCatalogListener(), this)
        mCategoriesAdapterObserver = CategoriesAdapterObserver(this)
        INSTANCE.addCategoriesUpdatesListener(mCategoriesAdapterObserver)
    }

    protected open fun onPrepareControllers(view: View) {
        mKmlImportController = KmlImportController(activity!!, this)
    }

    protected open fun updateLoadingPlaceholder() {
        val root = view ?: throw AssertionError("Fragment view must be non-null at this point!")
        val loadingPlaceholder =
            root.findViewById<View>(R.id.placeholder_loading)
        val showLoadingPlaceholder =
            INSTANCE.isAsyncBookmarksLoadingInProgress
        UiUtils.showIf(showLoadingPlaceholder, loadingPlaceholder)
    }

    override fun onStart() {
        super.onStart()
        INSTANCE.addLoadingListener(this)
        INSTANCE.addSharingListener(this)
        INSTANCE.addCatalogListener(mCatalogListener)
        if (mKmlImportController != null) mKmlImportController!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        INSTANCE.removeLoadingListener(this)
        INSTANCE.removeSharingListener(this)
        INSTANCE.removeCatalogListener(mCatalogListener)
        if (mKmlImportController != null) mKmlImportController!!.onStop()
    }

    override fun onResume() {
        super.onResume()
        updateLoadingPlaceholder()
        adapter!!.notifyDataSetChanged()
        if (!INSTANCE.isAsyncBookmarksLoadingInProgress) mImportKmlTask.run()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        INSTANCE.removeCategoriesUpdatesListener(mCategoriesAdapterObserver)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val processor =
            MenuItemClickProcessorWrapper.getInstance(item.itemId)
        processor.mInternalProcessor
            .process(this, getSelectedCategory())
        Statistics.INSTANCE.trackBookmarkListSettingsClick(processor.analytics)
        return true
    }

    protected fun showBottomMenu(item: BookmarkCategory) {
        mSelectedCategory = item
        showBottomMenuInternal(item)
    }

    private fun showBottomMenuInternal(item: BookmarkCategory) {
        val bs =
            BottomSheetHelper.create(activity!!, item.name)
                .sheet(categoryMenuResId)
                .listener(this)
        val bottomSheet = bs.build()
        prepareBottomMenuItems(bottomSheet)
        val menuItem =
            BottomSheetHelper.findItemById(bottomSheet, R.id.show_on_map)
        menuItem.setIcon(if (item.isVisible) R.drawable.ic_hide else R.drawable.ic_show)
            .setTitle(if (item.isVisible) R.string.hide else R.string.show)
        BottomSheetHelper.tint(bottomSheet)
        bottomSheet.show()
    }

    protected abstract fun prepareBottomMenuItems(bottomSheet: BottomSheet)
    @get:MenuRes
    protected open val categoryMenuResId: Int
        protected get() = R.menu.menu_bookmark_categories

    override fun onMoreOperationClick(item: BookmarkCategory) {
        showBottomMenu(item)
    }

    override fun setupPlaceholder(placeholder: PlaceholderView?) { // A placeholder is no needed on this screen.
    }

    override fun onBookmarksLoadingStarted() {
        updateLoadingPlaceholder()
    }

    override fun onBookmarksLoadingFinished() {
        updateLoadingPlaceholder()
        adapter?.notifyDataSetChanged()
        mImportKmlTask.run()
    }

    override fun onBookmarksFileLoaded(success: Boolean) { // Do nothing here.
    }

    private fun importKml() {
        if (mKmlImportController != null) mKmlImportController!!.importKml()
    }

    override fun onPreparedFileForSharing(result: BookmarkSharingResult) {
        SharingHelper.INSTANCE.onPreparedFileForSharing(activity!!, result)
    }

    override fun onFooterClick() {
        mCategoryEditor =
            object : CategoryEditor {
                override fun commit(newName: String) {
                    INSTANCE.createCategory(
                        newName
                    )
                }
            }
        EditTextDialogFragment.show(
            getString(R.string.bookmarks_create_new_group),
            getString(R.string.bookmarks_new_list_hint),
            getString(R.string.bookmark_set_name),
            getString(R.string.create),
            getString(R.string.cancel),
            MAX_CATEGORY_NAME_LENGTH,
            this
        )
    }

    override fun onFinishKmlImport() {
        adapter?.notifyDataSetChanged()
    }

    override val saveTextListener: OnTextSaveListener
        get() = object : OnTextSaveListener {
            override fun onSaveText(text: String) {
                this@BaseBookmarkCategoriesFragment.onSaveText(text)
            }
        }


    override val validator: EditTextDialogFragment.Validator
        get() = CategoryValidator()

    protected abstract val type: BookmarkCategory.Type

    override fun onItemClick(v: View, category: BookmarkCategory) {
        mSelectedCategory = category
        startActivityForResult(
            makeBookmarksListIntent(category),
            REQ_CODE_DELETE_CATEGORY
        )
    }

    private fun makeBookmarksListIntent(category: BookmarkCategory): Intent {
        return Intent(activity, BookmarkListActivity::class.java)
            .putExtra(BookmarksListFragment.EXTRA_CATEGORY, category)
    }

    protected open fun onShareActionSelected(category: BookmarkCategory) {
        SharingHelper.INSTANCE.prepareBookmarkCategoryForSharing(activity!!, category.id)
    }

    private fun onDeleteActionSelected(category: BookmarkCategory) {
        INSTANCE.deleteCategory(category.id)
        adapter?.notifyDataSetChanged()
        onDeleteActionSelected()
    }

    protected open fun onDeleteActionSelected() { // Do nothing.
    }

    protected open fun onActivityResultInternal(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) { // Do nothing.
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQ_CODE_DELETE_CATEGORY) {
            onDeleteActionSelected(getSelectedCategory())
            return
        }
        onActivityResultInternal(requestCode, resultCode, data)
    }

    override fun onItemLongClick(
        v: View,
        category: BookmarkCategory
    ) {
        showBottomMenu(category)
    }

    fun onSaveText(text: String) {
        mCategoryEditor?.commit(text)
        adapter?.notifyDataSetChanged()
    }

    open fun createCatalogListener(): BookmarksCatalogListener {
        return DefaultBookmarksCatalogListener()
    }

    protected open fun getSelectedCategory(): BookmarkCategory {
        if (mSelectedCategory == null) throw java.lang.AssertionError("Invalid attempt to use null selected category.")
        return mSelectedCategory as BookmarkCategory
    }

    internal interface CategoryEditor {
        fun commit(newName: String)
    }

    private inner class ImportKmlTask : Runnable {
        private var alreadyDone = false
        override fun run() {
            if (alreadyDone) return
            importKml()
            alreadyDone = true
        }
    }



    protected enum class MenuItemClickProcessorWrapper(
        @IdRes private val mId: Int, val mInternalProcessor: MenuClickProcessorBase,
        val analytics: Analytics
    ) {
        SET_SHARE(
            R.id.share,
            ShareAction(),
            Analytics(Statistics.ParamValue.SEND_AS_FILE)
        ),
        SET_EDIT(
            R.id.edit,
            EditAction(),
            Analytics(Statistics.ParamValue.EDIT)
        ),
        SHOW_ON_MAP(
            R.id.show_on_map,
            ShowAction(),
            Analytics(Statistics.ParamValue.MAKE_INVISIBLE_ON_MAP)
        ),
        SHARING_OPTIONS(
            R.id.sharing_options,
            OpenSharingOptions(),
            Analytics(Statistics.ParamValue.SHARING_OPTIONS)
        ),
        LIST_SETTINGS(
            R.id.settings,
            OpenListSettings(),
            Analytics(Statistics.ParamValue.LIST_SETTINGS)
        ),
        DELETE_LIST(
            R.id.delete,
            DeleteAction(),
            Analytics(Statistics.ParamValue.DELETE_GROUP)
        );

        companion object {

            fun getInstance(@IdRes resId: Int): MenuItemClickProcessorWrapper {
                for (each in values()) {
                    if (each.mId == resId) {
                        return each
                    }
                }
                throw IllegalArgumentException("Enum value for res id = $resId not found")
            }
        }

    }

    protected abstract class MenuClickProcessorBase {
        abstract fun process(
            frag: BaseBookmarkCategoriesFragment,
            category: BookmarkCategory
        )

        class ShowAction : MenuClickProcessorBase() {
            override fun process(
                frag: BaseBookmarkCategoriesFragment,
                category: BookmarkCategory
            ) {
                INSTANCE.toggleCategoryVisibility(category.id)
                frag.adapter!!.notifyDataSetChanged()
            }
        }

        class ShareAction : MenuClickProcessorBase() {
            override fun process(
                frag: BaseBookmarkCategoriesFragment,
                category: BookmarkCategory
            ) {
                frag.onShareActionSelected(category)
            }
        }

        class DeleteAction : MenuClickProcessorBase() {
            override fun process(
                frag: BaseBookmarkCategoriesFragment,
                category: BookmarkCategory
            ) {
                frag.onDeleteActionSelected(category)
            }
        }

        class EditAction : MenuClickProcessorBase() {
            override fun process(
                frag: BaseBookmarkCategoriesFragment,
                category: BookmarkCategory
            ) {
                frag.mCategoryEditor =
                    object : CategoryEditor {
                        override fun commit(newName: String) {
                            INSTANCE.setCategoryName(
                                category.id,
                                newName
                            )
                        }
                    }
                EditTextDialogFragment.show(
                    frag.getString(R.string.bookmark_set_name),
                    category.name,
                    frag.getString(R.string.rename),
                    frag.getString(R.string.cancel),
                    MAX_CATEGORY_NAME_LENGTH,
                    frag
                )
            }
        }

        class OpenSharingOptions : MenuClickProcessorBase() {
            override fun process(
                frag: BaseBookmarkCategoriesFragment,
                category: BookmarkCategory
            ) {
                UgcRouteSharingOptionsActivity.startForResult(frag.activity!!, category)
            }
        }

        class OpenListSettings : MenuClickProcessorBase() {
            override fun process(
                frag: BaseBookmarkCategoriesFragment,
                category: BookmarkCategory
            ) {
                UgcRouteEditSettingsActivity.startForResult(frag.activity!!, category)
            }
        }
    }

    private class CategoriesAdapterObserver internal constructor(fragment: BaseBookmarkCategoriesFragment) :
        DataChangedListener<BaseBookmarkCategoriesFragment> {
        private var mFragment: BaseBookmarkCategoriesFragment?
        override fun attach(`object`: BaseBookmarkCategoriesFragment) {
            mFragment = `object`
        }

        override fun detach() {
            mFragment = null
        }

        override fun onChanged() {
            if (mFragment == null) return
            val strategy = mFragment!!.type.filterStrategy
            val snapshot =
                INSTANCE.getCategoriesSnapshot(strategy)
            mFragment!!.adapter?.setItems(snapshot.items)
        }

        init {
            mFragment = fragment
        }
    }

    companion object {
        const val REQ_CODE_CATALOG = 101
        private const val REQ_CODE_DELETE_CATEGORY = 102
        private const val MAX_CATEGORY_NAME_LENGTH = 60
        fun setEnableForMenuItem(
            @IdRes id: Int, bottomSheet: BottomSheet,
            enable: Boolean
        ) {
            BottomSheetHelper
                .findItemById(bottomSheet, id)
                .setVisible(enable).isEnabled = enable
        }
    }
}