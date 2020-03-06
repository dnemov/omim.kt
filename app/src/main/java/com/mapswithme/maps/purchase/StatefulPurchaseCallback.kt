package com.mapswithme.maps.purchase

import com.mapswithme.maps.base.Detachable

internal abstract class StatefulPurchaseCallback<State, UiObject : PurchaseStateActivator<State>?> :
    Detachable<UiObject> {
    private var mPendingState: State? = null
    var uiObject: UiObject? = null
        private set

    fun activateStateSafely(state: State) {
        if (uiObject == null) {
            mPendingState = state
            return
        }
        uiObject!!.activateState(state)
    }

    override fun attach(uiObject: UiObject) {
        this.uiObject = uiObject
        if (mPendingState != null) {
            uiObject!!.activateState(mPendingState)
            mPendingState = null
        }
        onAttach(uiObject)
    }

    override fun detach() {
        onDetach()
        uiObject = null
    }

    open fun onAttach(uiObject: UiObject) { // Do nothing by default.
    }

    fun onDetach() { // Do nothing by default.
    }
}