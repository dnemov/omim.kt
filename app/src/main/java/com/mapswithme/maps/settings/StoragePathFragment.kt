package com.mapswithme.maps.settings

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.mapswithme.maps.R
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.maps.settings.StoragePathManager.MoveFilesListener
import com.mapswithme.util.Constants
import com.mapswithme.util.Utils
import java.util.*

class StoragePathFragment : BaseSettingsFragment(), MoveFilesListener,
    OnBackPressListener {
    private var mHeader: TextView? = null
    private var mList: ListView? = null
    private var mAdapter: StoragePathAdapter? = null
    private val mPathManager = StoragePathManager()
    protected override val layoutRes: Int
        protected get() = R.layout.fragment_prefs_storage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        mHeader = root!!.findViewById<View>(R.id.header) as TextView
        mList = root.findViewById<View>(R.id.list) as ListView
        mList!!.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                mAdapter!!.onItemClick(position)
            }
        return root
    }

    override fun onResume() {
        super.onResume()
        mPathManager.startExternalStorageWatching(
            activity,
            object: StoragePathManager.OnStorageListChangedListener {
                override fun onStorageListChanged(
                    storageItems: List<StorageItem>?,
                    currentStorageIndex: Int
                ) {
                    updateList()
                }
            },
            this
        )
        if (mAdapter == null) mAdapter = StoragePathAdapter(mPathManager, activity)
        updateList()
        mList!!.adapter = mAdapter
    }

    override fun onPause() {
        super.onPause()
        mPathManager.stopExternalStorageWatching()
    }

    private fun updateList() {
        val dirSize = StorageUtils.writableDirSize
        mHeader!!.text = getString(R.string.maps) + ": " + getSizeString(
            dirSize
        )
        if (mAdapter != null) mAdapter!!.update(
            mPathManager.storageItems,
            mPathManager.currentStorageIndex,
            dirSize
        )
    }

    override fun moveFilesFinished(newPath: String?) {
        updateList()
    }

    override fun moveFilesFailed(errorCode: Int) {
        if (!isAdded) return
        val message = "Failed to move maps with internal error: $errorCode"
        val activity: Activity? = activity
        if (activity!!.isFinishing) return
        AlertDialog.Builder(activity)
            .setTitle(message)
            .setPositiveButton(
                R.string.report_a_bug
            ) { dialog: DialogInterface?, which: Int ->
                Utils.sendBugReport(
                    activity,
                    message
                )
            }.show()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    companion object {
        fun getSizeString(size: Long): String {
            val units = arrayOf("Kb", "Mb", "Gb")
            var current = Constants.KB.toLong()
            var i = 0
            while (i < units.size) {
                val bound = Constants.KB * current
                if (size < bound) break
                current = bound
                ++i
            }
            // left 1 digit after the comma and add postfix string
            return String.format(
                Locale.US,
                "%.1f %s",
                size.toDouble() / current,
                units[i]
            )
        }
    }
}