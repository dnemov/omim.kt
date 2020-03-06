package com.mapswithme.maps.discovery

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

internal class LocalsError(
    @field:ErrorCode @get:ErrorCode
    @param:ErrorCode val code: Int, val message: String
) {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(UNKNOWN_ERROR)
    internal annotation class ErrorCode

    override fun toString(): String {
        return "LocalsError{" +
                "mCode=" + code +
                ", mMessage=" + message +
                '}'
    }

    companion object {
        const val UNKNOWN_ERROR = 0
    }

}