package com.mapswithme.maps.purchase

/**
 * Represents a billing connection abstraction.
 */
interface BillingConnection {
    /**
     * Opens a connection to the billing manager.
     */
    fun open()

    /**
     * Closes the connection to the billing manager.
     */
    fun close()

    /**
     * @return the connection state of the billing manager.
     */
    val state: State

    enum class State {
        DISCONNECTED, CONNECTING, CONNECTED, CLOSED
    }
}