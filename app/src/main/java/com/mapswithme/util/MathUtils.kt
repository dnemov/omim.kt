package com.mapswithme.util

import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

object MathUtils {
    fun average(vararg vals: Double): Double {
        var sum = 0.0
        for (`val` in vals) sum += `val`
        return sum / vals.size
    }
}