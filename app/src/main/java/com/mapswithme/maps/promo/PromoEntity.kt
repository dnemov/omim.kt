package com.mapswithme.maps.promo

import com.mapswithme.maps.gallery.RegularAdapterStrategy
import com.mapswithme.maps.promo.PromoCityGallery.LuxCategory

class PromoEntity(
    type: Int, title: String, subtitle: String?,
    url: String?, val category: LuxCategory?,
    val imageUrl: String?
) : RegularAdapterStrategy.Item(type, title, subtitle, url)