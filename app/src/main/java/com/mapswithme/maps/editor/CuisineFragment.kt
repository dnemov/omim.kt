package com.mapswithme.maps.editor

import com.mapswithme.maps.base.BaseMwmRecyclerFragment

class CuisineFragment : BaseMwmRecyclerFragment<CuisineAdapter?>() {
    private var mAdapter: CuisineAdapter? = null
    override fun createAdapter(): CuisineAdapter {
        mAdapter = CuisineAdapter()
        return mAdapter!!
    }

    val cuisines: Array<String>
        get() = mAdapter!!.cuisines

    fun setFilter(filter: String) {
        mAdapter!!.setFilter(filter)
    }
}