package com.mapswithme.util.statistics

interface StatisticValueConverter<T> {
    fun toStatisticValue(): T
}