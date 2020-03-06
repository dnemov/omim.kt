package com.mapswithme.maps.dialog

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.mapswithme.maps.R

object DialogUtils {
    private fun buildAlertDialog(context: Context, @StringRes titleId: Int): AlertDialog.Builder {
        return AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(titleId)
            .setPositiveButton(
                R.string.ok
            ) { dlg: DialogInterface, which: Int -> dlg.dismiss() }
    }

    fun buildAlertDialog(
        context: Context, @StringRes titleId: Int,
        @StringRes msgId: Int
    ): AlertDialog.Builder {
        return buildAlertDialog(context, titleId)
            .setMessage(msgId)
    }

    private fun buildAlertDialog(
        context: Context, @StringRes titleId: Int,
        msg: CharSequence, @StringRes posBtn: Int,
        posClickListener: DialogInterface.OnClickListener,
        @StringRes negBtn: Int,
        negClickListener: DialogInterface.OnClickListener?
    ): AlertDialog.Builder {
        return buildAlertDialog(context, titleId, msg, posBtn, posClickListener)
            .setNegativeButton(negBtn, negClickListener)
    }

    private fun buildAlertDialog(
        context: Context, @StringRes titleId: Int,
        msg: CharSequence, @StringRes posBtn: Int,
        posClickListener: DialogInterface.OnClickListener
    ): AlertDialog.Builder {
        return buildAlertDialog(context, titleId)
            .setMessage(msg)
            .setPositiveButton(posBtn, posClickListener)
    }

    @JvmStatic
    fun showAlertDialog(
        context: Context, @StringRes titleId: Int,
        @StringRes msgId: Int
    ) {
        buildAlertDialog(context, titleId, msgId).show()
    }

    @JvmStatic
    fun showAlertDialog(context: Context, @StringRes titleId: Int) {
        buildAlertDialog(context, titleId).show()
    }

    fun showAlertDialog(
        context: Context, @StringRes titleId: Int,
        @StringRes msgId: Int, @StringRes posBtn: Int,
        posClickListener: DialogInterface.OnClickListener,
        @StringRes negBtn: Int
    ) {
        buildAlertDialog(
            context, titleId, context.getString(msgId), posBtn, posClickListener, negBtn,
            null
        ).show()
    }

    fun showAlertDialog(
        context: Context, @StringRes titleId: Int,
        msg: CharSequence, @StringRes posBtn: Int,
        posClickListener: DialogInterface.OnClickListener,
        @StringRes negBtn: Int
    ) {
        buildAlertDialog(context, titleId, msg, posBtn, posClickListener, negBtn, null)
            .show()
    }

    fun showAlertDialog(
        context: Context, @StringRes titleId: Int,
        msg: CharSequence, @StringRes posBtn: Int,
        posClickListener: DialogInterface.OnClickListener,
        @StringRes negBtn: Int,
        negClickListener: DialogInterface.OnClickListener?
    ) {
        buildAlertDialog(
            context,
            titleId,
            msg,
            posBtn,
            posClickListener,
            negBtn,
            negClickListener
        ).show()
    }

    fun showAlertDialog(
        context: Context, @StringRes titleId: Int,
        @StringRes msgId: Int, @StringRes posBtn: Int,
        posClickListener: DialogInterface.OnClickListener,
        @StringRes negBtn: Int,
        negClickListener: DialogInterface.OnClickListener?
    ) {
        buildAlertDialog(
            context, titleId, context.getString(msgId), posBtn, posClickListener, negBtn,
            negClickListener
        ).show()
    }

    fun showAlertDialog(
        context: Context, @StringRes titleId: Int,
        @StringRes msgId: Int, @StringRes posBtn: Int,
        posClickListener: DialogInterface.OnClickListener
    ) {
        buildAlertDialog(
            context,
            titleId,
            context.getString(msgId),
            posBtn,
            posClickListener
        ).show()
    }

    @JvmStatic
    fun createModalProgressDialog(context: Context, @StringRes msg: Int): ProgressDialog {
        val progress = ProgressDialog(context, R.style.MwmTheme_AlertDialog)
        progress.setMessage(context.getString(msg))
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progress.isIndeterminate = true
        progress.setCancelable(false)
        return progress
    }

    fun createModalProgressDialog(
        context: Context, @StringRes msg: Int,
        whichButton: Int, @StringRes buttonText: Int,
        clickListener: DialogInterface.OnClickListener?
    ): ProgressDialog {
        val progress = createModalProgressDialog(context, msg)
        progress.setButton(whichButton, context.getString(buttonText), clickListener)
        return progress
    }
}