package com.mapswithme.util

import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import java.util.*

/**
 * `Registrator` pattern implementation which allows to maintain the list of listeners,
 * offers safe adding/removing of listeners during iteration.
 * <br></br>[.finishIterate] must be called after iteration is complete.
 */
class Listeners<T> : Iterable<T> {
    private val mListeners: MutableSet<T> = LinkedHashSet()
    private val mListenersToAdd: MutableSet<T> = LinkedHashSet()
    private val mListenersToRemove: MutableSet<T> = LinkedHashSet()
    private var mIterating = false
    override fun iterator(): MutableIterator<T> {
        if (mIterating) throw RuntimeException("finishIterate() must be called before new iteration")
        mIterating = true
        return mListeners.iterator()
    }

    /**
     * Completes listeners iteration. Must be called after iteration is done.
     */
    fun finishIterate() {
        if (!mListenersToRemove.isEmpty()) mListeners.removeAll(mListenersToRemove)
        if (!mListenersToAdd.isEmpty()) mListeners.addAll(mListenersToAdd)
        mListenersToAdd.clear()
        mListenersToRemove.clear()
        mIterating = false
    }

    /**
     * Safely registers new listener. If registered during iteration, new listener will NOT be called before current iteration is complete.
     */
    fun register(listener: T) {
        if (mIterating) {
            mListenersToRemove.remove(listener)
            if (!mListeners.contains(listener)) mListenersToAdd.add(listener)
        } else mListeners.add(listener)
    }

    /**
     * Safely unregisters listener. If unregistered during iteration, old listener WILL be called in the current iteration.
     */
    fun unregister(listener: T) {
        if (mIterating) {
            mListenersToAdd.remove(listener)
            if (mListeners.contains(listener)) mListenersToRemove.add(listener)
        } else mListeners.remove(listener)
    }

    fun getSize(): Int {
        var res = mListeners.size
        if (mIterating) {
            res += mListenersToAdd.size
            res -= mListenersToRemove.size
        }
        return res
    }

    fun isEmpty(): Boolean {
        return getSize() <= 0
    }
}