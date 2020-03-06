package com.mapswithme.maps.permissions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import java.util.*

@Suppress("CAST_NEVER_SUCCEEDS")
internal class PermissionsAdapter :
    RecyclerView.Adapter<PermissionsAdapter.ViewHolder?>() {
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TYPE_TITLE,
        TYPE_PERMISSION,
        TYPE_NOTE
    )
    internal annotation class ViewHolderType

    companion object {
        const val TYPE_TITLE = 0
        const val TYPE_PERMISSION = 1
        const val TYPE_NOTE = 2
        private var ITEMS: List<PermissionItem>? = null

        init {
            val items = ArrayList<PermissionItem>()
            items.add(
                PermissionItem(
                    TYPE_TITLE,
                    R.string.onboarding_detail_permissions_title,
                    0,
                    0
                )
            )
            items.add(
                PermissionItem(
                    TYPE_PERMISSION,
                    R.string.onboarding_detail_permissions_storage_title,
                    R.string.onboarding_detail_permissions_storage_message,
                    R.drawable.ic_storage_permission
                )
            )
            items.add(
                PermissionItem(
                    TYPE_PERMISSION,
                    R.string.onboarding_detail_permissions_location_title,
                    R.string.onboarding_detail_permissions_location_message,
                    R.drawable.ic_navigation_permission
                )
            )
            items.add(
                PermissionItem(
                    TYPE_NOTE, 0,
                    R.string.onboarding_detail_permissions_storage_path_message, 0
                )
            )
            ITEMS =
                Collections.unmodifiableList(items)
        }
    }

    @ViewHolderType
    override fun getItemViewType(position: Int): Int {
        return ITEMS!![position].mType
    }

    override fun onCreateViewHolder(parent: ViewGroup, @ViewHolderType viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_NOTE -> NoteViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_permissions_note, parent, false)
            )
            TYPE_PERMISSION -> PermissionViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_permission, parent,
                        false
                    )
            )
            TYPE_TITLE -> TitleViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_permissions_title, parent,
                        false
                    )
            )
            else -> null as ViewHolder
        }
    }


    override fun getItemCount(): Int {
        return ITEMS!!.size
    }

    class PermissionItem internal constructor(
        @ViewHolderType val mType: Int, @StringRes val mTitle: Int, @StringRes val mMessage: Int, @field:DrawableRes val mIcon: Int
    )

    internal abstract class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: PermissionItem)
    }

    private class TitleViewHolder internal constructor(itemView: View) :
        ViewHolder(itemView) {
        private val mTitle: TextView
        override fun bind(item: PermissionItem) {
            mTitle.setText(item.mTitle)
        }

        init {
            mTitle = itemView.findViewById<View>(R.id.tv__title) as TextView
        }
    }

    private class PermissionViewHolder internal constructor(itemView: View) :
        ViewHolder(itemView) {
        private val mIcon: ImageView
        private val mTitle: TextView
        private val mMessage: TextView
        override fun bind(item: PermissionItem) {
            mIcon.setImageResource(item.mIcon)
            mTitle.setText(item.mTitle)
            mMessage.setText(item.mMessage)
        }

        init {
            mIcon =
                itemView.findViewById<View>(R.id.iv__permission_icon) as ImageView
            mTitle = itemView.findViewById<View>(R.id.tv__permission_title) as TextView
            mMessage =
                itemView.findViewById<View>(R.id.tv__permission_message) as TextView
        }
    }

    private class NoteViewHolder internal constructor(itemView: View) :
        ViewHolder(itemView) {
        private val mMessage = itemView.findViewById<View>(R.id.tv__note) as TextView
        override fun bind(item: PermissionItem) {
            mMessage.setText(item.mMessage)
        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ITEMS!![position])
    }
}