package com.mapswithme.maps.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.editor.data.FeatureCategory
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.util.Language
import com.mapswithme.util.Utils
import java.util.*

class FeatureCategoryFragment : BaseMwmRecyclerFragment<FeatureCategoryAdapter?>() {
    private var mSelectedCategory: FeatureCategory? = null
    protected var mToolbarController: ToolbarController? = null

    interface FeatureCategoryListener {
        fun onFeatureCategorySelected(category: FeatureCategory?)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null && arguments!!.containsKey(FeatureCategoryActivity.Companion.EXTRA_FEATURE_CATEGORY)) mSelectedCategory =
            arguments!!.getParcelable(FeatureCategoryActivity.Companion.EXTRA_FEATURE_CATEGORY)
        mToolbarController = object : SearchToolbarController(view, activity) {
            override fun onTextChanged(query: String) {
                setFilter(query)
            }
        }
    }

    private fun setFilter(query: String) {
        val locale = Language.defaultLocale
        val creatableTypes =
            if (query.isEmpty()) Editor.nativeGetAllCreatableFeatureTypes(
                locale
            ) else Editor.nativeSearchCreatableFeatureTypes(
                query,
                locale
            )
        val categories =
            makeFeatureCategoriesFromTypes(creatableTypes)
        adapter!!.setCategories(categories)
    }

    override fun createAdapter(): FeatureCategoryAdapter {
        val locale = Language.defaultLocale
        val creatableTypes =
            Editor.nativeGetAllCreatableFeatureTypes(locale)
        val categories =
            makeFeatureCategoriesFromTypes(creatableTypes)
        return FeatureCategoryAdapter(this, categories, mSelectedCategory)
    }

    @Suppress("UNCHECKED_CAST")
    private fun makeFeatureCategoriesFromTypes(creatableTypes: Array<String>): Array<FeatureCategory> {
        val categories =
            arrayOfNulls<FeatureCategory>(creatableTypes.size)
        for (i in creatableTypes.indices) {
            val localizedType = Utils.getLocalizedFeatureType(
                context!!,
                creatableTypes[i]
            )
            categories[i] = FeatureCategory(creatableTypes[i], localizedType)
        }
        Arrays.sort(
            categories
        ) { lhs: FeatureCategory?, rhs: FeatureCategory? ->
            lhs!!.localizedTypeName.compareTo(rhs!!.localizedTypeName)
        }
        return categories as Array<FeatureCategory>
    }

    fun selectCategory(category: FeatureCategory?) {
        if (activity is FeatureCategoryListener) (activity as FeatureCategoryListener?)!!.onFeatureCategorySelected(
            category
        ) else if (parentFragment is FeatureCategoryListener) (parentFragment as FeatureCategoryListener?)!!.onFeatureCategorySelected(
            category
        )
    }
}