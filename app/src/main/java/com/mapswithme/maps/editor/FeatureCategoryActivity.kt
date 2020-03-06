package com.mapswithme.maps.editor

import android.content.Intent
import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.editor.FeatureCategoryFragment.FeatureCategoryListener
import com.mapswithme.maps.editor.data.FeatureCategory

class FeatureCategoryActivity : BaseMwmFragmentActivity(), FeatureCategoryListener {
    override val fragmentClass: Class<out Fragment>
        protected get() = FeatureCategoryFragment::class.java

    override fun onFeatureCategorySelected(category: FeatureCategory?) {
        Editor.createMapObject(category)
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra(EXTRA_FEATURE_CATEGORY, category)
        intent.putExtra(EditorActivity.Companion.EXTRA_NEW_OBJECT, true)
        startActivity(intent)
    }

    companion object {
        const val EXTRA_FEATURE_CATEGORY = "FeatureCategory"
    }
}