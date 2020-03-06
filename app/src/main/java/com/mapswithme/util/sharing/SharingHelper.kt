package com.mapswithme.util.sharing

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import android.text.TextUtils

import com.cocosw.bottomsheet.BottomSheet
import com.google.gson.Gson
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkSharingResult
import com.mapswithme.util.BottomSheetHelper
import com.mapswithme.maps.dialog.DialogUtils
import com.mapswithme.util.concurrency.ThreadPool
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory

import java.io.File
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet

enum class SharingHelper {
    INSTANCE;

    private val mPrefs = MwmApplication.get().getSharedPreferences(SharingHelper.PREFS_STORAGE, Context.MODE_PRIVATE)
    private val mItems = HashMap<String?, SharingTarget>()

    private var mProgressDialog: ProgressDialog? = null

    fun initialize() {
        ThreadPool.storage.execute {
            val items: Array<SharingTarget>?
            val json = INSTANCE.mPrefs.getString(PREFS_KEY_ITEMS, null)
            items = parse(json)

            if (items != null) {
                for (item in items)
                    INSTANCE.mItems[item.packageName] = item
            }
        }
    }

    private fun save() {
        val json = Gson().toJson(mItems.values)
        mPrefs.edit().putString(PREFS_KEY_ITEMS, json).apply()
    }

    private fun findItems(data: BaseShareable): List<SharingTarget> {
        val missed = HashSet(mItems.keys)

        val it = data.getTargetIntent(null)
        val pm = MwmApplication.get().packageManager
        val rlist = pm.queryIntentActivities(it, 0)

        val res = ArrayList<SharingTarget>(rlist.size)
        for (ri in rlist) {
            val ai = ri.activityInfo ?: continue

            missed.remove(ai.packageName)
            val target = SharingTarget(ai.packageName)
            target.name = guessAppName(pm, ri)
            target.activityName = ai.name

            val original = mItems[ai.packageName]
            if (original != null)
                target.usageCount = original.usageCount

            target.drawableIcon = ai.loadIcon(pm)

            res.add(target)
        }

        Collections.sort(res) { obj, another -> obj.compareTo(another) }

        for (item in missed)
            mItems.remove(item)

        if (!missed.isEmpty())
            save()

        return res
    }

    private fun updateItem(item: SharingTarget) {
        var stored: SharingTarget? = mItems[item.packageName]
        if (stored == null) {
            stored = SharingTarget(item.packageName)
            mItems[stored.packageName] = stored
        }

        stored.usageCount++
        save()
    }

    fun prepareBookmarkCategoryForSharing(context: Activity, catId: Long) {
        mProgressDialog = DialogUtils.createModalProgressDialog(context, R.string.please_wait)
        mProgressDialog!!.show()
        BookmarkManager.INSTANCE.prepareCategoryForSharing(catId)
    }

    fun onPreparedFileForSharing(
        context: Activity,
        result: BookmarkSharingResult
    ) {
        if (mProgressDialog != null && mProgressDialog!!.isShowing)
            mProgressDialog!!.dismiss()
        shareBookmarksCategory(context, result)
    }

    companion object {

        private val LOGGER = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = SharingHelper::class.java.simpleName
        private val PREFS_STORAGE = "sharing"

        private val PREFS_KEY_ITEMS = "items"
        private val KMZ_MIME_TYPE = "application/vnd.google-earth.kmz"

        private fun parse(json: String?): Array<SharingTarget>? {
            if (TextUtils.isEmpty(json))
                return null

            try {
                return Gson().fromJson(json, Array<SharingTarget>::class.java)
            } catch (e: Exception) {
                return null
            }

        }

        private fun guessAppName(pm: PackageManager, ri: ResolveInfo): String {
            var name = ri.activityInfo.loadLabel(pm)

            if (TextUtils.isEmpty(name)) {
                name = ri.loadLabel(pm)

                if (TextUtils.isEmpty(name))
                    name = ri.activityInfo.packageName
            }

            return name.toString()
        }

        @JvmOverloads
        fun shareOutside(data: BaseShareable, @StringRes titleRes: Int = R.string.share) {
            shareInternal(data, titleRes, INSTANCE.findItems(data))
        }

        private fun shareInternal(data: BaseShareable, titleRes: Int, items: List<SharingTarget>) {
            val builder = BottomSheetHelper.createGrid(data.activity, titleRes)
                .limit(R.integer.sharing_initial_rows)

            var i = 0
            for (item in items)
                builder.sheet(i++, item.drawableIcon!!, item.name!!)

            builder.listener { dialog, which ->
                if (which > 0) {
                    val target = items[which]
                    INSTANCE.updateItem(target)

                    data.share(target)
                }
            }

            UiThread.runLater(Runnable { builder.show() }, 500)
        }

        private fun shareBookmarksCategory(
            context: Activity,
            result: BookmarkSharingResult
        ) {
            when (result.code) {
                BookmarkSharingResult.SUCCESS -> {
                    val name = File(result.sharingPath).name
                    shareOutside(
                        LocalFileShareable(context, result.sharingPath, KMZ_MIME_TYPE)
                            .setText(context.getString(R.string.share_bookmarks_email_body))
                            .setSubject(R.string.share_bookmarks_email_subject)
                    )
                }
                BookmarkSharingResult.EMPTY_CATEGORY -> DialogUtils.showAlertDialog(
                    context, R.string.bookmarks_error_title_share_empty,
                    R.string.bookmarks_error_message_share_empty
                )
                BookmarkSharingResult.ARCHIVE_ERROR, BookmarkSharingResult.FILE_ERROR -> {
                    DialogUtils.showAlertDialog(
                        context, R.string.dialog_routing_system_error,
                        R.string.bookmarks_error_message_share_general
                    )
                    val catName = BookmarkManager.INSTANCE.getCategoryById(result.categoryId).name
                    LOGGER.e(
                        TAG, "Failed to share bookmark category '" + catName + "', error code: "
                                + result.code
                    )
                }
                else -> throw AssertionError("Unsupported bookmark sharing code: " + result.code)
            }
        }

        fun shareViralEditor(context: Activity?, @DrawableRes imageId: Int, @StringRes subject: Int, @StringRes text: Int) {
            shareOutside(
                ViralEditorShareable(context, imageId)
                    .setText(text)
                    .setSubject(subject)
            )
        }
    }

}
