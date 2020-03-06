package com.mapswithme.maps.settings

import com.mapswithme.util.Constants

/**
 * Represents storage option.
 */
class StorageItem internal constructor(// Path to the root of writable directory.
    val mPath: String?, // Free size.
    val mFreeSize: Long
) {

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o == null || o !is StorageItem) return false
        val other = o
        // Storage equal is considered equal, either its path OR size equals to another one's.
// Size of storage free space can change dynamically, so that hack provides us with better results identifying the same storages.
        return mFreeSize == other.mFreeSize || mPath == other.mPath
    }

    override fun hashCode(): Int { // Yes, do not put StorageItem to Hash containers, performance will be awful.
// At least such hash is compatible with hacky equals.
        return 0
    }

    override fun toString(): String {
        return "$mPath, $mFreeSize"
    }

    val fullPath: String
        get() = mPath + Constants.MWM_DIR_POSTFIX

}