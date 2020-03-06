package com.mapswithme.maps.widget.recycler

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.mapswithme.maps.R
import java.util.*

object ItemDecoratorFactory {
    fun createHotelGalleryDecorator(
        context: Context,
        orientation: Int
    ): ItemDecoration {
        val decoration: DividerItemDecoration =
            HotelDividerItemDecoration(context, orientation)
        @DrawableRes val dividerId = R.drawable.divider_transparent_quarter
        decoration.setDrawable(
            ContextCompat.getDrawable(context, dividerId)!!
        )
        return decoration
    }

    fun createSponsoredGalleryDecorator(
        context: Context,
        orientation: Int
    ): ItemDecoration {
        val decoration: DividerItemDecoration =
            SponsoredDividerItemDecoration(context, orientation)
        @DrawableRes val dividerId = R.drawable.divider_transparent_half
        decoration.setDrawable(
            ContextCompat.getDrawable(context, dividerId)!!
        )
        return decoration
    }

    fun createPlacePagePromoGalleryDecorator(
        context: Context,
        orientation: Int
    ): ItemDecoration {
        val decoration: DividerItemDecoration =
            SponsoredDividerItemDecoration(context, orientation)
        @DrawableRes val dividerId = R.drawable.divider_transparent_quarter
        decoration.setDrawable(
            ContextCompat.getDrawable(context, dividerId)!!
        )
        return decoration
    }

    fun createRatingRecordDecorator(
        context: Context,
        orientation: Int,
        @DrawableRes dividerResId: Int
    ): ItemDecoration {
        val decoration =
            DividerItemDecoration(context, orientation)
        decoration.setDrawable(
            ContextCompat.getDrawable(context, dividerResId)!!
        )
        return decoration
    }

    @JvmStatic
    fun createDefaultDecorator(
        context: Context,
        orientation: Int
    ): ItemDecoration {
        return DividerItemDecoration(context, orientation)
    }

    fun createVerticalDefaultDecorator(context: Context): ItemDecoration {
        return DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        )
    }

    fun createRatingRecordDecorator(
        context: Context,
        horizontal: Int
    ): ItemDecoration {
        return createRatingRecordDecorator(
            context,
            horizontal,
            R.drawable.divider_transparent_base
        )
    }
}