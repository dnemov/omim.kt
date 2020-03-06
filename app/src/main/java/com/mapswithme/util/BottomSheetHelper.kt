package com.mapswithme.util

import android.app.Activity
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.MenuItem
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.cocosw.bottomsheet.BottomSheet
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

object BottomSheetHelper {
    @JvmStatic
    fun tint(bottomSheet: BottomSheet) {
        for (i in 0 until bottomSheet.getMenu().size()) {
            val mi: MenuItem = bottomSheet.getMenu().getItem(i)
            val icon = mi.icon
            if (icon != null) mi.setIcon(Graphics.tint(bottomSheet.getContext(), icon))
        }
    }

    fun findItemById(bottomSheet: BottomSheet, @IdRes id: Int): MenuItem {
        return bottomSheet.getMenu().findItem(id)
            ?: throw AssertionError("Can not find bottom sheet item with id: $id")
    }

    fun create(context: Activity): Builder {
        return Builder(context)
    }

    fun create(context: Activity, @StringRes title: Int): Builder {
        return create(context).title(title)
    }

    fun create(
        context: Activity,
        title: CharSequence?
    ): Builder {
        return create(context).title(title)
    }

    fun createGrid(context: Activity, @StringRes title: Int): Builder {
        return create(context, title).grid()
    }

    fun sheet(
        builder: Builder,
        id: Int, @DrawableRes iconRes: Int,
        text: CharSequence
    ): Builder {
        val icon: Drawable =
            ContextCompat.getDrawable(MwmApplication.get(), iconRes)!!
        return builder.sheet(id, icon, text)
    }

    class Builder(context: Activity) : BottomSheet.Builder(context) {
        override fun build(): BottomSheet {
            return super.build()
        }

        fun setOnDismissListener(listener: DialogInterface.OnDismissListener?): Builder {
            super.setOnDismissListener(DialogInterface.OnDismissListener { dialog ->
                listener?.onDismiss(
                    dialog
                )
            })
            return this
        }

        override fun title(title: CharSequence?): Builder {
            super.title(title)
            return this
        }

        override fun title(@StringRes title: Int): Builder {
            super.title(title)
            return this
        }

        override fun sheet(@MenuRes xmlRes: Int): Builder {
            super.sheet(xmlRes)
            return this
        }

        override fun sheet(
            id: Int,
            icon: Drawable,
            text: CharSequence
        ): Builder {
            super.sheet(id, icon, text)
            return this
        }

        override fun sheet(id: Int, @DrawableRes iconRes: Int, @StringRes textRes: Int): Builder {
            super.sheet(id, iconRes, textRes)
            return this
        }

        override fun grid(): Builder {
            super.grid()
            return this
        }

        override fun listener(listener: MenuItem.OnMenuItemClickListener): Builder {
            super.listener(listener)
            return this
        }

        init {
            setOnDismissListener(null)
            if (ThemeUtils.isNightTheme) darkTheme()
        }
    }
}