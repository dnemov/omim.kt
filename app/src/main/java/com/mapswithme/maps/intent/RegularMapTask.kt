package com.mapswithme.maps.intent

abstract class RegularMapTask : MapTask {
    override fun toStatisticValue(): String {
        throw UnsupportedOperationException("This task '$this' not tracked in statistic!")
    }

    companion object {
        private const val serialVersionUID = -6799622370628032853L
    }
}