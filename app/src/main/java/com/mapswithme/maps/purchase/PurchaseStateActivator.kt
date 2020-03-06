package com.mapswithme.maps.purchase

interface PurchaseStateActivator<State> {
    fun activateState(state: State?)
}