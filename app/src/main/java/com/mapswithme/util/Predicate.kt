package com.mapswithme.util

import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

abstract class Predicate<T, D> internal constructor(val baseValue: T) {

    abstract fun apply(field: D): Boolean
    class Equals<T, D>(converter: TypeConverter<D, T>, data: T) :
        Predicate<T, D>(data) {
        private val mConverter: TypeConverter<D, T>
        override fun apply(field: D): Boolean {
            val converted: T = mConverter.convert(field)
            val value = baseValue
            return value === converted || value == converted
        }

        init {
            mConverter = converter
        }
    }

}