package com.mapswithme.util

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.DrawableCompat
import com.mapswithme.maps.R
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

object Graphics {
    fun drawCircle(
        color: Int, @DimenRes sizeResId: Int,
        res: Resources
    ): Drawable {
        val size = res.getDimensionPixelSize(sizeResId)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        paint.color = color
        paint.isAntiAlias = true
        val c = Canvas(bmp)
        val radius = size / 2.0f
        c.drawCircle(radius, radius, radius, paint)
        return BitmapDrawable(res, bmp)
    }

    fun drawCircleAndImage(
        color: Int, @DimenRes sizeResId: Int,
        @DrawableRes imageResId: Int, @DimenRes sizeImgResId: Int,
        res: Resources
    ): Drawable {
        val size = res.getDimensionPixelSize(sizeResId)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        paint.color = color
        paint.isAntiAlias = true
        val c = Canvas(bmp)
        val radius = size / 2.0f
        c.drawCircle(radius, radius, radius, paint)
        val imgD = res.getDrawable(imageResId)
        imgD.mutate()
        val sizeImg = res.getDimensionPixelSize(sizeImgResId)
        val offset = (size - sizeImg) / 2
        imgD.setBounds(offset, offset, size - offset, size - offset)
        imgD.draw(c)
        return BitmapDrawable(res, bmp)
    }

    @JvmOverloads
    fun tint(view: TextView, @AttrRes tintAttr: Int = R.attr.iconTint) {
        val dlist = view.compoundDrawables
        for (i in dlist.indices) dlist[i] =
            tint(view.context, dlist[i], tintAttr)
        view.setCompoundDrawablesWithIntrinsicBounds(
            dlist[0],
            dlist[1],
            dlist[2],
            dlist[3]
        )
    }

    fun tint(view: TextView, tintColors: ColorStateList?) {
        val dlist = view.compoundDrawables
        for (i in dlist.indices) dlist[i] = tint(dlist[i], tintColors)
        view.setCompoundDrawablesWithIntrinsicBounds(
            dlist[0],
            dlist[1],
            dlist[2],
            dlist[3]
        )
    }

    @JvmOverloads
    fun tint(context: Context, @DrawableRes resId: Int, @AttrRes tintAttr: Int = R.attr.iconTint): Drawable? {
        return tint(context, context.resources.getDrawable(resId), tintAttr)
    }

    @JvmOverloads
    fun tint(
        context: Context,
        drawable: Drawable?, @AttrRes tintAttr: Int = R.attr.iconTint
    ): Drawable? {
        return tint(drawable, ThemeUtils.getColor(context, tintAttr))
    }

    fun tint(src: Drawable?, @ColorInt color: Int): Drawable? {
        if (src == null) return null
        if (color == Color.TRANSPARENT) return src
        val tmp = src.bounds
        val res: Drawable = DrawableCompat.wrap(src)
        DrawableCompat.setTint(res.mutate(), color)
        res.bounds = tmp
        return res
    }

    fun tint(
        src: Drawable?,
        tintColors: ColorStateList?
    ): Drawable? {
        if (src == null) return null
        val tmp = src.bounds
        val res: Drawable = DrawableCompat.wrap(src)
        DrawableCompat.setTintList(res.mutate(), tintColors)
        res.bounds = tmp
        return res
    }
}