package com.mapswithme.util

import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

class Option<T>(useIfValueIsNull: T, value: T) {
    private val mValue: T?
    private val mOption: T
    fun hasValue(): Boolean {
        return mValue != null
    }

    fun get(): T? {
        return if (hasValue()) mValue else mOption
    }

    init {
        mValue = value
        mOption = useIfValueIsNull
    }
}