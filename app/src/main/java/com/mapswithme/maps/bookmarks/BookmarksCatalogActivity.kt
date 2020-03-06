package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseToolbarActivity

class BookmarksCatalogActivity : BaseToolbarActivity() {
    override fun setupHomeButton(toolbar: Toolbar) {}
    override val fragmentClass: Class<out Fragment>
        protected get() = BookmarksCatalogFragment::class.java

    override fun onHomeOptionItemSelected() {
        finish()
    }

    companion object {
        const val EXTRA_DOWNLOADED_CATEGORY = "extra_downloaded_category"
        fun startForResult(
            fragment: Fragment, requestCode: Int,
            catalogUrl: String
        ) {
            fragment.startActivityForResult(
                makeLaunchIntent(
                    fragment.requireContext(),
                    catalogUrl
                ),
                requestCode
            )
        }

        @JvmStatic
        fun startForResult(
            context: Activity, requestCode: Int,
            catalogUrl: String
        ) {
            context.startActivityForResult(
                makeLaunchIntent(
                    context,
                    catalogUrl
                ), requestCode
            )
        }

        @JvmStatic
        fun start(context: Context, catalogUrl: String) {
            context.startActivity(
                makeLaunchIntent(
                    context,
                    catalogUrl
                )
            )
        }

        private fun makeLaunchIntent(
            context: Context,
            catalogUrl: String
        ): Intent {
            val intent = Intent(context, BookmarksCatalogActivity::class.java)
            intent.putExtra(
                BookmarksCatalogFragment.Companion.EXTRA_BOOKMARKS_CATALOG_URL,
                catalogUrl
            )
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            return intent
        }
    }
}