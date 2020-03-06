package com.mapswithme.maps.editor

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.dialog.EditTextDialogFragment
import com.mapswithme.maps.dialog.EditTextDialogFragment.EditTextDialogInterface
import com.mapswithme.maps.dialog.EditTextDialogFragment.OnTextSaveListener
import com.mapswithme.maps.editor.data.LocalizedStreet

class StreetFragment : BaseMwmRecyclerFragment<StreetAdapter?>(),
    EditTextDialogInterface {
    private var mSelectedString: LocalizedStreet? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        mSelectedString = Editor.nativeGetStreet()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Editor.nativeSetStreet(street)
    }

    override fun createAdapter(): StreetAdapter {
        return StreetAdapter(
            this,
            Editor.nativeGetNearbyStreets(),
            mSelectedString!!
        )
    }

    val street: LocalizedStreet
        get() = (adapter as StreetAdapter).selectedStreet

    fun saveStreet(street: LocalizedStreet?) {
        if (parentFragment is EditorHostFragment) (parentFragment as EditorHostFragment?)!!.setStreet(
            street
        )
    }

    override val saveTextListener: OnTextSaveListener
        get() = object : OnTextSaveListener {
            override fun onSaveText(text: String) {
                saveStreet(LocalizedStreet(text, ""))
            }
        }

    override val validator: EditTextDialogFragment.Validator
        get() = object : EditTextDialogFragment.Validator {
            override fun validate(activity: Activity, text: String?): Boolean {
                return !TextUtils.isEmpty(text)
            }
        }
}