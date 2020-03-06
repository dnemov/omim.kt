package com.mapswithme.maps.dialog

internal class DefaultDialogFactory : DialogFactory {
    override fun createDialog(): AlertDialog {
        return AlertDialog()
    }
}