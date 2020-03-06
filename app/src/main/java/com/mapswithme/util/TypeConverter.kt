package com.mapswithme.util

interface TypeConverter<D, T> {
    fun convert(data: D): T
}