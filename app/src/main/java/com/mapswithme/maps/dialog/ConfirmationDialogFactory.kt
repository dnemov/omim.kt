package com.mapswithme.maps.dialog

class ConfirmationDialogFactory : DialogFactory {
    override fun createDialog(): AlertDialog {
        return DefaultConfirmationAlertDialog()
    }
}