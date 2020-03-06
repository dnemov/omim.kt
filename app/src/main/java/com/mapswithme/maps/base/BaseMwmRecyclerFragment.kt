package com.mapswithme.maps.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.PlaceholderView
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import org.alohalytics.Statistics

abstract class BaseMwmRecyclerFragment<T : RecyclerView.Adapter<*>?> :
    Fragment() {
    var toolbar: Toolbar? = null
        private set
    lateinit var recyclerView: RecyclerView
        private set
    private var mPlaceholder: PlaceholderView? = null
    var adapter: T? = null

    protected abstract fun createAdapter(): T
    @get:LayoutRes
    protected open val layoutRes: Int
        protected get() = R.layout.fragment_recycler

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Utils.detachFragmentIfCoreNotInitialized(context, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutRes, container, false)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.toolbar)
        if (toolbar != null) {
            UiUtils.showHomeUpButton(toolbar!!)
            toolbar!!.setNavigationOnClickListener { v: View? ->
                Utils.navigateToParent(
                    activity
                )
            }
        }
        recyclerView = view.findViewById(R.id.recycler)
        checkNotNull(recyclerView) { "RecyclerView not found in layout" }
        val manager =
            LinearLayoutManager(view.context)
        manager.isSmoothScrollbarEnabled = true
        recyclerView.layoutManager = manager
        adapter = createAdapter()
        recyclerView.adapter = adapter
        mPlaceholder = view.findViewById(R.id.placeholder)
        setupPlaceholder(mPlaceholder)
    }

    fun requirePlaceholder(): PlaceholderView? {
        if (mPlaceholder != null) return mPlaceholder
        throw IllegalStateException("Placeholder not found in layout")
    }

    override fun onResume() {
        super.onResume()
        Statistics.logEvent(
            "\$onResume", this.javaClass.simpleName
                    + ":" + UiUtils.deviceOrientationAsString(activity!!)
        )
    }

    override fun onPause() {
        super.onPause()
        Statistics.logEvent("\$onPause", this.javaClass.simpleName)
    }

    protected open fun setupPlaceholder(placeholder: PlaceholderView?) {}
    fun setupPlaceholder() {
        setupPlaceholder(mPlaceholder)
    }

    fun showPlaceholder(show: Boolean) {
        if (mPlaceholder != null) UiUtils.showIf(show, mPlaceholder!!)
    }
}