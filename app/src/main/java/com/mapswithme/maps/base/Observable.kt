package com.mapswithme.maps.base

import java.util.*

abstract class Observable<T : DataChangedListener<*>?> {
    private val mListeners: MutableList<T> = ArrayList()
    fun registerListener(listener: T) {
        check(!mListeners.contains(listener)) { "Observer $listener is already registered." }
        mListeners.add(listener)
    }

    fun unregisterListener(listener: T) {
        val index = mListeners.indexOf(listener)
        check(index != -1) { "Observer $listener was not registered." }
        mListeners.removeAt(index)
    }

    protected fun notifyChanged() {
        for (item in mListeners) item!!.onChanged()
    }
}