package com.mapswithme.maps.dialog

interface AlertDialogCallback {
    /**
     *
     * @param requestCode the code that the dialog was launched with.
     * @param which indicates which button was pressed, for details see
     * [android.content.DialogInterface]
     *
     * @see android.content.DialogInterface.OnClickListener
     */
    fun onAlertDialogPositiveClick(requestCode: Int, which: Int)

    /**
     *
     * @param requestCode the code that the dialog was launched with.
     * @param which indicates which button was pressed, for details see
     * [android.content.DialogInterface]
     *
     * @see android.content.DialogInterface.OnClickListener
     */
    fun onAlertDialogNegativeClick(requestCode: Int, which: Int)

    /**
     * Called when the dialog is cancelled.
     *
     * @param requestCode the code that the dialog was launched with.
     */
    fun onAlertDialogCancel(requestCode: Int)
}