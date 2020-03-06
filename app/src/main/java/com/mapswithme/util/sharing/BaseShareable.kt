package com.mapswithme.util.sharing

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.TextUtils
import androidx.annotation.StringRes
import com.mapswithme.util.statistics.AlohaHelper.logException

abstract class BaseShareable(val activity: Activity) {
    protected var baseIntent: Intent
    protected var mText: String? = null
    protected var mSubject: String? = null

    protected open fun modifyIntent(intent: Intent?, target: SharingTarget?) {}

    fun getTargetIntent(target: SharingTarget?): Intent {
        val res = baseIntent
        if (!TextUtils.isEmpty(mText)) res.putExtra(Intent.EXTRA_TEXT, mText)
        if (!TextUtils.isEmpty(mSubject)) res.putExtra(Intent.EXTRA_SUBJECT, mSubject)
        val mime = mimeType
        if (!TextUtils.isEmpty(mime)) res.type = mime
        modifyIntent(res, target)
        return res
    }

    open fun share(target: SharingTarget) {
        val intent = getTargetIntent(target)
        target.setupComponentName(intent)
        try {
            activity.startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
            logException(ignored)
        }
    }

    fun setBaseIntent(intent: Intent): BaseShareable {
        baseIntent = intent
        return this
    }

    fun setText(text: String?): BaseShareable {
        mText = text
        return this
    }

    fun setSubject(subject: String?): BaseShareable {
        mSubject = subject
        return this
    }

    fun setText(@StringRes textRes: Int): BaseShareable {
        mText = activity.getString(textRes)
        return this
    }

    fun setSubject(@StringRes subjectRes: Int): BaseShareable {
        mSubject = activity.getString(subjectRes)
        return this
    }

    abstract val mimeType: String

    init {
        baseIntent = Intent(Intent.ACTION_SEND)
    }
}