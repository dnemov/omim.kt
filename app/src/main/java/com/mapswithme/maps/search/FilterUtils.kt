package com.mapswithme.maps.search

import androidx.annotation.IntDef
import com.mapswithme.maps.search.HotelsFilter
import com.mapswithme.maps.search.HotelsFilter.*
import com.mapswithme.maps.search.PriceFilterView
import com.mapswithme.maps.search.PriceFilterView.PriceDef
import com.mapswithme.maps.search.RatingFilterView
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

object FilterUtils {
    const val RATING_ANY = 0
    const val RATING_GOOD = 1
    const val RATING_VERYGOOD = 2
    const val RATING_EXCELLENT = 3
    fun makeOneOf(iterator: Iterator<HotelType?>): OneOf? {
        if (!iterator.hasNext()) return null
        val type = iterator.next()
        return OneOf(type!!, makeOneOf(iterator))
    }

    fun combineFilters(vararg filters: HotelsFilter): HotelsFilter? {
        var result: HotelsFilter? = null
        for (filter in filters) {
            if (result == null) {
                result = filter
                continue
            }
            if (filter != null) result = And(filter, result)
        }
        return result
    }

    fun findPriceFilter(filter: HotelsFilter): HotelsFilter? {
        if (filter is PriceRateFilter) return filter
        if (filter is Or) {
            val or = filter
            if (or.mLhs is PriceRateFilter
                && or.mRhs is PriceRateFilter
            ) {
                return filter
            }
            if (or.mLhs is Or
                && or.mLhs.mLhs is PriceRateFilter
                && or.mLhs.mRhs is PriceRateFilter
            ) {
                return filter
            }
        }
        var result: HotelsFilter?
        if (filter is And) {
            val and = filter
            result = findPriceFilter(and.mLhs)
            if (result == null) result = findPriceFilter(and.mRhs)
            return result
        }
        return null
    }

    fun findTypeFilter(filter: HotelsFilter): OneOf? {
        if (filter is OneOf) return filter
        var result: OneOf?
        if (filter is And) {
            val and = filter
            result = findTypeFilter(and.mLhs)
            if (result == null) result = findTypeFilter(and.mRhs)
            return result
        }
        return null
    }

    fun findRatingFilter(filter: HotelsFilter): RatingFilter? {
        if (filter is RatingFilter) return filter
        var result: RatingFilter?
        if (filter is And) {
            val and = filter
            result = findRatingFilter(and.mLhs)
            if (result == null) result = findRatingFilter(and.mRhs)
            return result
        }
        return null
    }

    @JvmStatic
    fun createHotelFilter(
        @RatingDef rating: Int, priceRate: Int,
        vararg types: HotelType?
    ): HotelsFilter? {
        val ratingFilter = createRatingFilter(rating)
        val priceFilter = createPriceRateFilter(priceRate)
        val typesFilter = createHotelTypeFilter(*types)
        return combineFilters(ratingFilter!!, priceFilter!!, typesFilter!!)
    }

    private fun createRatingFilter(@RatingDef rating: Int): HotelsFilter? {
        return when (rating) {
            RATING_ANY -> null
            RATING_GOOD -> RatingFilter(
                Op.OP_GE,
                RatingFilterView.GOOD
            )
            RATING_VERYGOOD -> RatingFilter(
                Op.OP_GE,
                RatingFilterView.VERY_GOOD
            )
            RATING_EXCELLENT -> RatingFilter(
                Op.OP_GE,
                RatingFilterView.EXCELLENT
            )
            else -> throw AssertionError("Unsupported rating type: $rating")
        }
    }

    private fun createPriceRateFilter(@PriceDef priceRate: Int): HotelsFilter? {
        return if (priceRate != PriceFilterView.LOW && priceRate != PriceFilterView.MEDIUM && priceRate != PriceFilterView.HIGH) null else PriceRateFilter(
            Op.OP_EQ,
            priceRate
        )
    }

    private fun createHotelTypeFilter(vararg types: HotelType?): HotelsFilter? {
        if (types == null) return null
        val hotelTypes: List<HotelType> =
            ArrayList<HotelType>(Arrays.asList(*types))
        return makeOneOf(hotelTypes.iterator())
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        RATING_ANY,
        RATING_GOOD,
        RATING_VERYGOOD,
        RATING_EXCELLENT
    )
    annotation class RatingDef
}