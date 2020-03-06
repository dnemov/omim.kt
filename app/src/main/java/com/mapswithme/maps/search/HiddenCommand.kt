package com.mapswithme.maps.search

interface HiddenCommand {
    /**
     * Executes the specified command.
     *
     * @return true if the command has been executed, otherwise - false.
     */
    fun execute(command: String): Boolean

    abstract class BaseHiddenCommand internal constructor(private val mCommand: String) :
        HiddenCommand {
        override fun execute(command: String): Boolean {
            if (mCommand != command) return false
            executeInternal()
            return true
        }

        abstract fun executeInternal()

    }
}