package com.mapswithme.maps.dialog

import android.app.Dialog
import android.os.Bundle

interface ResolveDialogViewStrategy {
    fun createView(
        dialog: AlertDialog,
        args: Bundle
    ): Dialog
}