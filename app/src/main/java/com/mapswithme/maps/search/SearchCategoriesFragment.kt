package com.mapswithme.maps.search

import android.os.Bundle
import android.view.View
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.purchase.AdsRemovalActivationCallback
import com.mapswithme.maps.purchase.AdsRemovalPurchaseDialog
import com.mapswithme.maps.search.CategoriesAdapter.CategoriesUiListener

class SearchCategoriesFragment : BaseMwmRecyclerFragment<CategoriesAdapter?>(),
    CategoriesUiListener, AdsRemovalActivationCallback {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        adapter!!.updateCategories(this)
    }

    override fun createAdapter(): CategoriesAdapter {
        return CategoriesAdapter(this)
    }

    override val layoutRes: Int
        protected get() = R.layout.fragment_search_categories

    protected fun safeOnActivityCreated(savedInstanceState: Bundle?) {
        (parentFragment as SearchFragment?)!!.setRecyclerScrollListener(recyclerView)
    }

    override fun onSearchCategorySelected(category: String) {
        if (!passCategory(
                parentFragment,
                category
            )
        ) passCategory(activity, category)
    }

    override fun onPromoCategorySelected(promo: PromoCategory) {
        val processor = promo.createProcessor(activity!!)
        processor.process()
    }

    override fun onAdsRemovalSelected() {
        AdsRemovalPurchaseDialog.show(this)
    }

    override fun onAdsRemovalActivation() {
        adapter!!.updateCategories(this)
        adapter!!.notifyDataSetChanged()
    }

    companion object {
        private fun passCategory(listener: Any?, category: String?): Boolean {
            if (listener !is CategoriesUiListener) return false
            listener.onSearchCategorySelected(category!!)
            return true
        }
    }
}