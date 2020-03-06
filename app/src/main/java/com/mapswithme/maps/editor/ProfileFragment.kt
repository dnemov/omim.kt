package com.mapswithme.maps.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.mapswithme.maps.R
import com.mapswithme.maps.editor.OsmOAuth.OnUserStatsChanged
import com.mapswithme.maps.editor.ProfileFragment
import com.mapswithme.maps.editor.data.UserStats
import com.mapswithme.util.BottomSheetHelper
import com.mapswithme.util.Constants
import com.mapswithme.util.UiUtils
import java.util.*

class ProfileFragment : AuthFragment(), View.OnClickListener,
    OnUserStatsChanged {
    private var mSentBlock: View? = null
    private var mEditsSent: TextView? = null
    private var mEditsSentDate: TextView? = null
    private var mMore: View? = null
    private var mAuthBlock: View? = null
    private var mRatingBlock: View? = null
    private var mEditorRank: TextView? = null
    private val mEditorLevelUp: TextView? = null

    private enum class MenuItem(@field:DrawableRes @param:DrawableRes val icon: Int, @field:StringRes @param:StringRes val title: Int) {
        LOGOUT(R.drawable.ic_logout, R.string.logout) {
            override operator fun invoke(fragment: ProfileFragment) {
                AlertDialog.Builder(fragment.context!!)
                    .setMessage(R.string.are_you_sure)
                    .setPositiveButton(
                        android.R.string.ok
                    ) { dialog, which ->
                        OsmOAuth.clearAuthorization()
                        fragment.refreshViews()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .create()
                    .show()
            }
        },
        REFRESH(R.drawable.ic_update, R.string.refresh) {
            override operator fun invoke(fragment: ProfileFragment) {
                OsmOAuth.nativeUpdateOsmUserStats(OsmOAuth.username, true /* forceUpdate */)
            }
        };

        abstract operator fun invoke(fragment: ProfileFragment)

    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbarController.setTitle(R.string.profile)
        initViews(view)
        refreshViews()
        OsmOAuth.setUserStatsListener(this)
        OsmOAuth.nativeUpdateOsmUserStats(OsmOAuth.username, false /* forceUpdate */)
    }

    private fun initViews(view: View) {
        mMore = toolbarController.toolbar.findViewById(R.id.more)
        mMore?.setOnClickListener(this)
        val editsBlock = view.findViewById<View>(R.id.block_edits)
        UiUtils.show(editsBlock)
        mSentBlock = editsBlock.findViewById(R.id.sent_edits)
        mEditsSent = mSentBlock?.findViewById<View>(R.id.edits_count) as TextView
        mEditsSentDate = mSentBlock?.findViewById<View>(R.id.date_sent) as TextView
        mAuthBlock = view.findViewById(R.id.block_auth)
        mRatingBlock = view.findViewById(R.id.block_rating)
        mEditorRank = mRatingBlock?.findViewById<View>(R.id.rating) as TextView
        // FIXME show when it will be implemented on server
//    mEditorLevelUp = mRatingBlock.findViewById(R.id.level_up_feat);
        view.findViewById<View>(R.id.about_osm).setOnClickListener(this)
    }

    private fun refreshViews() {
        if (OsmOAuth.isAuthorized) {
            UiUtils.show(mMore, mRatingBlock, mSentBlock)
            UiUtils.hide(mAuthBlock!!)
        } else {
            UiUtils.show(mAuthBlock)
            UiUtils.hide(mMore, mRatingBlock, mSentBlock)
        }
        refreshRatings(0, 0, 0, "")
    }

    private fun refreshRatings(
        uploadedCount: Long,
        uploadSeconds: Long,
        rank: Long,
        levelFeat: String?
    ) {
        val edits: String
        val editsDate: String
        if (uploadedCount == 0L) {
            editsDate = "---"
            edits = editsDate
        } else {
            edits = uploadedCount.toString()
            editsDate = DateUtils.formatDateTime(
                activity,
                uploadSeconds * 1000,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
            )
        }
        mEditsSent!!.text = edits
        mEditsSentDate!!.text = getString(R.string.last_update, editsDate)
        mEditorRank!!.text = rank.toString()
        // FIXME show when it will be implemented on server
//    mEditorLevelUp.setText(levelFeat);
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.more -> showBottomSheet()
            R.id.about_osm -> startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(Constants.Url.OSM_ABOUT)
                )
            )
        }
    }

    override fun onStatsChange(stats: UserStats?) {
        if (!isAdded) return
        if (stats == null) refreshRatings(
            0,
            0,
            0,
            ""
        ) else refreshRatings(
            stats.editsCount.toLong(),
            stats.uploadTimestampSeconds,
            stats.editorRank.toLong(),
            stats.levelUp
        )
    }

    private fun showBottomSheet() {
        val items: MutableList<MenuItem> =
            ArrayList()
        items.add(MenuItem.REFRESH)
        items.add(MenuItem.LOGOUT)
        val bs =
            BottomSheetHelper.create(activity!!)
        for (item in items) bs.sheet(
            item.ordinal,
            item.icon,
            item.title
        )
        val bottomSheet =
            bs.listener { item ->
                MenuItem.values()[item.itemId].invoke(this@ProfileFragment)
                false
            }.build()
        BottomSheetHelper.tint(bottomSheet)
        bottomSheet.show()
    }
}