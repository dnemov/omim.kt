package com.mapswithme.maps.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.mapswithme.maps.R
import com.mapswithme.maps.editor.data.LocalizedName
import com.mapswithme.util.StringUtils.SimpleTextWatcher
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils

class MultilanguageAdapter internal constructor(hostFragment: EditorHostFragment?) :
    RecyclerView.Adapter<MultilanguageAdapter.Holder>() {
    private val mNames: List<LocalizedName>?
    val mandatoryNamesCount: Int
    private var mAdditionalLanguagesShown = false
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_localized_name, parent, false)
        // TODO(mgsergio): Deletion is not implemented.
        UiUtils.hide(view.findViewById(R.id.delete))
        return Holder(view)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        val name = mNames!![position]
        holder.input.setText(name.name)
        holder.inputLayout.hint = name.langName
    }

    override fun getItemCount(): Int {
        return if (mAdditionalLanguagesShown) mNames!!.size else mandatoryNamesCount
    }

    val namesCount: Int
        get() = mNames!!.size

    fun getNameAtPos(pos: Int): LocalizedName {
        return mNames!![pos]
    }

    fun areAdditionalLanguagesShown(): Boolean {
        return mAdditionalLanguagesShown
    }

    fun showAdditionalLanguages(show: Boolean) {
        if (mAdditionalLanguagesShown == show) return
        mAdditionalLanguagesShown = show
        if (mNames!!.size != mandatoryNamesCount) {
            if (show) {
                notifyItemRangeInserted(mandatoryNamesCount, mNames.size - mandatoryNamesCount)
            } else {
                notifyItemRangeRemoved(mandatoryNamesCount, mNames.size - mandatoryNamesCount)
            }
        }
    }

    inner class Holder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var input: EditText
        var inputLayout: TextInputLayout

        init {
            input = itemView.findViewById<View>(R.id.input) as EditText
            inputLayout =
                itemView.findViewById<View>(R.id.input_layout) as TextInputLayout
            input.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    UiUtils.setInputError(
                        inputLayout,
                        if (Editor.nativeIsNameValid(s.toString())) Utils.INVALID_ID else R.string.error_enter_correct_name
                    )
                    mNames!![adapterPosition].name = s.toString()
                }
            })
            itemView.findViewById<View>(R.id.delete)
                .setOnClickListener {
                    // TODO(mgsergio): Implement item deletion.
// int position = getAdapterPosition();
// mHostFragment.removeLocalizedName(position + 1);
// mNames.remove(position);
// notifyItemRemoved(position);
                }
        }
    }

    init {
        mNames = hostFragment!!.names
        mandatoryNamesCount = hostFragment.mandatoryNamesCount
    }
}