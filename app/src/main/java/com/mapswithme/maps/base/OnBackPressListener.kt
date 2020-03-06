package com.mapswithme.maps.base

interface OnBackPressListener {
    /**
     * Fragment tries to process back button press.
     *
     * @return true, if back was processed & fragment shouldn't be closed. false otherwise.
     */
    fun onBackPressed(): Boolean
}