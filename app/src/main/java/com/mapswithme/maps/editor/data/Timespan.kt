package com.mapswithme.maps.editor.data

class Timespan(val start: HoursMinutes?, val end: HoursMinutes?) {
    override fun toString(): String {
        return start.toString() + "-" + end
    }

}