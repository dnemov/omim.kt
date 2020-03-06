package com.mapswithme.maps.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.textfield.TextInputLayout
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.bookmarks.data.Metadata.MetadataType
import com.mapswithme.maps.dialog.EditTextDialogFragment
import com.mapswithme.maps.dialog.EditTextDialogFragment.EditTextDialogInterface
import com.mapswithme.maps.dialog.EditTextDialogFragment.OnTextSaveListener
import com.mapswithme.maps.editor.data.TimeFormatUtils
import com.mapswithme.util.*
import com.mapswithme.util.StringUtils.SimpleTextWatcher
import org.solovyev.android.views.llm.LinearLayoutManager

class EditorFragment : BaseMwmFragment(), View.OnClickListener,
    EditTextDialogInterface {
    private var mCategory: TextView? = null
    private var mCardName: View? = null
    private var mCardAddress: View? = null
    private var mCardMetadata: View? = null
    private var mNamesView: RecyclerView? = null
    private val mNamesObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            refreshNamesCaption()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            refreshNamesCaption()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            refreshNamesCaption()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            refreshNamesCaption()
        }

        override fun onItemRangeMoved(
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int
        ) {
            refreshNamesCaption()
        }
    }
    private var mNamesAdapter: MultilanguageAdapter? = null
    private var mNamesCaption: TextView? = null
    private var mAddLanguage: TextView? = null
    private var mMoreLanguages: TextView? = null
    private var mStreet: TextView? = null
    private var mHouseNumber: EditText? = null
    private var mZipcode: EditText? = null
    private var mBlockLevels: View? = null
    private var mBuildingLevels: EditText? = null
    private var mPhone: EditText? = null
    private var mWebsite: EditText? = null
    private var mEmail: EditText? = null
    private var mCuisine: TextView? = null
    private var mOperator: EditText? = null
    private var mWifi: SwitchCompat? = null
    private var mInputHouseNumber: TextInputLayout? = null
    private var mInputBuildingLevels: TextInputLayout? = null
    private var mInputZipcode: TextInputLayout? = null
    private var mInputPhone: TextInputLayout? = null
    private var mInputWebsite: TextInputLayout? = null
    private var mInputEmail: TextInputLayout? = null
    private var mEmptyOpeningHours: View? = null
    private var mOpeningHours: TextView? = null
    private var mEditOpeningHours: View? = null
    private var mDescription: EditText? = null
    private val mMetaBlocks = SparseArray<View>(7)
    private var mReset: TextView? = null
    private var mParent: EditorHostFragment? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editor, container, false)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        mParent = parentFragment as EditorHostFragment?
        initViews(view)
        mCategory!!.text = Utils.getLocalizedFeatureType(
            context!!,
            Editor.nativeGetCategory()
        )
        val street = Editor.nativeGetStreet()
        mStreet!!.text = street!!.defaultName
        mHouseNumber!!.setText(Editor.nativeGetHouseNumber())
        mHouseNumber!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                UiUtils.setInputError(
                    mInputHouseNumber!!,
                    if (Editor.nativeIsHouseValid(s.toString())) 0 else R.string.error_enter_correct_house_number
                )
            }
        })
        mZipcode!!.setText(Editor.nativeGetZipCode())
        mZipcode!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                UiUtils.setInputError(
                    mInputZipcode!!,
                    if (Editor.nativeIsZipcodeValid(s.toString())) 0 else R.string.error_enter_correct_zip_code
                )
            }
        })
        mBuildingLevels!!.setText(Editor.nativeGetBuildingLevels())
        mBuildingLevels!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                UiUtils.setInputError(
                    mInputBuildingLevels!!,
                    if (Editor.nativeIsLevelValid(s.toString())) 0 else R.string.error_enter_correct_storey_number
                )
            }
        })
        mPhone!!.setText(Editor.nativeGetPhone())
        mPhone!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                UiUtils.setInputError(
                    mInputPhone!!,
                    if (Editor.nativeIsPhoneValid(s.toString())) 0 else R.string.error_enter_correct_phone
                )
            }
        })
        mWebsite!!.setText(Editor.nativeGetWebsite())
        mWebsite!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                UiUtils.setInputError(
                    mInputWebsite!!,
                    if (Editor.nativeIsWebsiteValid(s.toString())) 0 else R.string.error_enter_correct_web
                )
            }
        })
        mEmail!!.setText(Editor.nativeGetEmail())
        mEmail!!.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                UiUtils.setInputError(
                    mInputEmail!!,
                    if (Editor.nativeIsEmailValid(s.toString())) 0 else R.string.error_enter_correct_email
                )
            }
        })
        mCuisine!!.text = Editor.nativeGetFormattedCuisine()
        mOperator!!.setText(Editor.nativeGetOperator())
        mWifi!!.isChecked = Editor.nativeHasWifi()
        refreshOpeningTime()
        refreshEditableFields()
        refreshResetButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        setEdits()
    }

    fun setEdits(): Boolean {
        if (!validateFields()) return false
        Editor.nativeSetHouseNumber(mHouseNumber!!.text.toString())
        Editor.nativeSetZipCode(mZipcode!!.text.toString())
        Editor.nativeSetBuildingLevels(mBuildingLevels!!.text.toString())
        Editor.nativeSetPhone(mPhone!!.text.toString())
        Editor.nativeSetWebsite(mWebsite!!.text.toString())
        Editor.nativeSetEmail(mEmail!!.text.toString())
        Editor.nativeSetHasWifi(mWifi!!.isChecked)
        Editor.nativeSetOperator(mOperator!!.text.toString())
        Editor.nativeSetNames(mParent!!.namesAsArray)
        return true
    }

    val description: String
        get() = mDescription!!.text.toString().trim { it <= ' ' }

    private fun validateFields(): Boolean {
        if (Editor.nativeIsAddressEditable()) {
            if (!Editor.nativeIsHouseValid(mHouseNumber!!.text.toString())) {
                mHouseNumber!!.requestFocus()
                InputUtils.showKeyboard(mHouseNumber)
                return false
            }
            if (!Editor.nativeIsLevelValid(mBuildingLevels!!.text.toString())) {
                mBuildingLevels!!.requestFocus()
                InputUtils.showKeyboard(mBuildingLevels)
                return false
            }
        }
        if (!Editor.nativeIsZipcodeValid(mZipcode!!.text.toString())) {
            mZipcode!!.requestFocus()
            InputUtils.showKeyboard(mZipcode)
            return false
        }
        if (!Editor.nativeIsPhoneValid(mPhone!!.text.toString())) {
            mPhone!!.requestFocus()
            InputUtils.showKeyboard(mPhone)
            return false
        }
        if (!Editor.nativeIsWebsiteValid(mWebsite!!.text.toString())) {
            mWebsite!!.requestFocus()
            InputUtils.showKeyboard(mWebsite)
            return false
        }
        if (!Editor.nativeIsEmailValid(mEmail!!.text.toString())) {
            mEmail!!.requestFocus()
            InputUtils.showKeyboard(mEmail)
            return false
        }
        return validateNames()
    }

    private fun validateNames(): Boolean {
        for (pos in 0 until mNamesAdapter!!.itemCount) {
            val localizedName = mNamesAdapter!!.getNameAtPos(pos)
            if (Editor.nativeIsNameValid(localizedName.name)) continue
            val nameView = mNamesView!!.getChildAt(pos)
            nameView.requestFocus()
            InputUtils.showKeyboard(nameView)
            return false
        }
        return true
    }

    private fun refreshEditableFields() {
        UiUtils.showIf(Editor.nativeIsNameEditable(), mCardName)
        UiUtils.showIf(Editor.nativeIsAddressEditable(), mCardAddress)
        UiUtils.showIf(
            Editor.nativeIsBuilding() && !Editor.nativeIsPointType(),
            mBlockLevels
        )
        val editableMeta = Editor.nativeGetEditableFields()
        if (editableMeta.isEmpty()) {
            UiUtils.hide(mCardMetadata!!)
            return
        }
        for (i in 0 until mMetaBlocks.size()) UiUtils.hide(mMetaBlocks.valueAt(i))
        var anyEditableMeta = false
        for (type in editableMeta) {
            val metaBlock = mMetaBlocks[type] ?: continue
            anyEditableMeta = true
            UiUtils.show(metaBlock)
        }
        UiUtils.showIf(anyEditableMeta, mCardMetadata)
    }

    private fun refreshOpeningTime() {
        val openingHours = Editor.nativeGetOpeningHours()
        if (TextUtils.isEmpty(openingHours) || !OpeningHours.nativeIsTimetableStringValid(
                openingHours
            )
        ) {
            UiUtils.show(mEmptyOpeningHours)
            UiUtils.hide(mOpeningHours, mEditOpeningHours)
        } else {
            val timetables =
                OpeningHours.nativeTimetablesFromString(openingHours)
            val content =
                if (timetables == null) openingHours else TimeFormatUtils.formatTimetables(
                    timetables
                )
            UiUtils.hide(mEmptyOpeningHours!!)
            UiUtils.setTextAndShow(mOpeningHours, content)
            UiUtils.show(mEditOpeningHours)
        }
    }

    private fun initNamesView(view: View) {
        mNamesCaption =
            view.findViewById<View>(R.id.show_additional_names) as TextView
        mNamesCaption!!.setOnClickListener(this)
        mAddLanguage = view.findViewById<View>(R.id.add_langs) as TextView
        mAddLanguage!!.setOnClickListener(this)
        mMoreLanguages = view.findViewById<View>(R.id.more_names) as TextView
        mMoreLanguages!!.setOnClickListener(this)
        mNamesView = view.findViewById<View>(R.id.recycler) as RecyclerView
        mNamesView!!.isNestedScrollingEnabled = false
        mNamesView!!.layoutManager = LinearLayoutManager(activity)
        mNamesAdapter = MultilanguageAdapter(mParent)
        mNamesView!!.adapter = mNamesAdapter
        mNamesAdapter!!.registerAdapterDataObserver(mNamesObserver)
        val args = arguments
        if (args == null || !args.containsKey(LAST_INDEX_OF_NAMES_ARRAY)) {
            showAdditionalNames(false)
            return
        }
        showAdditionalNames(true)

        UiUtils.waitLayout(mNamesView!!, ViewTreeObserver.OnGlobalLayoutListener {
            val lm =
                mNamesView!!.layoutManager as LinearLayoutManager?
            val position = args.getInt(LAST_INDEX_OF_NAMES_ARRAY)
            val nameItem = lm!!.findViewByPosition(position)
            val cvNameTop = mCardName!!.top
            val nameItemTop = nameItem!!.top
            view.scrollTo(0, cvNameTop + nameItemTop)
            // TODO(mgsergio): Uncomment if focus and keyboard are required.
            // TODO(mgsergio): Keyboard doesn't want to hide. Only pressing back button works.
            // View nameItemInput = nameItem.findViewById(R.id.input);
            // nameItemInput.requestFocus();
            // InputUtils.showKeyboard(nameItemInput);
        })
    }

    private fun initViews(view: View) {
        val categoryBlock = view.findViewById<View>(R.id.category)
        categoryBlock.setOnClickListener(this)
        // TODO show icon and fill it when core will implement that
        UiUtils.hide(categoryBlock.findViewById(R.id.icon))
        mCategory = categoryBlock.findViewById<View>(R.id.name) as TextView
        mCardName = view.findViewById(R.id.cv__name)
        mCardAddress = view.findViewById(R.id.cv__address)
        mCardMetadata = view.findViewById(R.id.cv__metadata)
        initNamesView(view)
        // Address
        view.findViewById<View>(R.id.block_street).setOnClickListener(this)
        mStreet = view.findViewById<View>(R.id.street) as TextView
        val blockHouseNumber =
            view.findViewById<View>(R.id.block_building)
        mHouseNumber = findInputAndInitBlock(blockHouseNumber, 0, R.string.house_number)
        mInputHouseNumber =
            blockHouseNumber.findViewById<View>(R.id.custom_input) as TextInputLayout
        val blockZipcode =
            view.findViewById<View>(R.id.block_zipcode)
        mZipcode = findInputAndInitBlock(blockZipcode, 0, R.string.editor_zip_code)
        mInputZipcode =
            blockZipcode.findViewById<View>(R.id.custom_input) as TextInputLayout
        // Details
        mBlockLevels = view.findViewById(R.id.block_levels)
        mBuildingLevels = findInputAndInitBlock(
            mBlockLevels,
            0,
            getString(R.string.editor_storey_number, 25)
        )
        mBuildingLevels!!.inputType = InputType.TYPE_CLASS_NUMBER
        mInputBuildingLevels =
            mBlockLevels?.findViewById<View>(R.id.custom_input) as TextInputLayout
        val blockPhone = view.findViewById<View>(R.id.block_phone)
        mPhone = findInputAndInitBlock(blockPhone, R.drawable.ic_phone, R.string.phone)
        mPhone!!.inputType = InputType.TYPE_CLASS_PHONE
        mInputPhone =
            blockPhone.findViewById<View>(R.id.custom_input) as TextInputLayout
        val blockWeb = view.findViewById<View>(R.id.block_website)
        mWebsite = findInputAndInitBlock(blockWeb, R.drawable.ic_website, R.string.website)
        mWebsite!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        mInputWebsite =
            blockWeb.findViewById<View>(R.id.custom_input) as TextInputLayout
        val blockEmail = view.findViewById<View>(R.id.block_email)
        mEmail = findInputAndInitBlock(blockEmail, R.drawable.ic_email, R.string.email)
        mEmail!!.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        mInputEmail =
            blockEmail.findViewById<View>(R.id.custom_input) as TextInputLayout
        val blockCuisine =
            view.findViewById<View>(R.id.block_cuisine)
        blockCuisine.setOnClickListener(this)
        mCuisine = view.findViewById<View>(R.id.cuisine) as TextView
        val blockOperator =
            view.findViewById<View>(R.id.block_operator)
        mOperator =
            findInputAndInitBlock(blockOperator, R.drawable.ic_operator, R.string.editor_operator)
        val blockWifi = view.findViewById<View>(R.id.block_wifi)
        mWifi = view.findViewById<View>(R.id.sw__wifi) as SwitchCompat
        blockWifi.setOnClickListener(this)
        val blockOpeningHours =
            view.findViewById<View>(R.id.block_opening_hours)
        mEditOpeningHours =
            blockOpeningHours.findViewById(R.id.edit_opening_hours)
        mEditOpeningHours?.setOnClickListener(this)
        mEmptyOpeningHours =
            blockOpeningHours.findViewById(R.id.empty_opening_hours)
        mEmptyOpeningHours?.setOnClickListener(this)
        mOpeningHours =
            blockOpeningHours.findViewById<View>(R.id.opening_hours) as TextView
        mOpeningHours!!.setOnClickListener(this)
        val cardMore = view.findViewById<View>(R.id.cv__more)
        mDescription = findInput(cardMore)
        cardMore.findViewById<View>(R.id.about_osm).setOnClickListener(this)
        mReset = view.findViewById<View>(R.id.reset) as TextView
        mReset!!.setOnClickListener(this)
        mMetaBlocks.append(MetadataType.FMD_OPEN_HOURS.toInt(), blockOpeningHours)
        mMetaBlocks.append(MetadataType.FMD_PHONE_NUMBER.toInt(), blockPhone)
        mMetaBlocks.append(MetadataType.FMD_WEBSITE.toInt(), blockWeb)
        mMetaBlocks.append(MetadataType.FMD_EMAIL.toInt(), blockEmail)
        mMetaBlocks.append(MetadataType.FMD_CUISINE.toInt(), blockCuisine)
        mMetaBlocks.append(MetadataType.FMD_OPERATOR.toInt(), blockOperator)
        mMetaBlocks.append(MetadataType.FMD_INTERNET.toInt(), blockWifi)
    }

    private fun findInputAndInitBlock(blockWithInput: View, @DrawableRes icon: Int, @StringRes hint: Int): EditText {
        return findInputAndInitBlock(blockWithInput, icon, getString(hint))
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.edit_opening_hours, R.id.empty_opening_hours, R.id.opening_hours -> mParent!!.editTimetable()
            R.id.block_wifi -> mWifi!!.toggle()
            R.id.block_street -> mParent!!.editStreet()
            R.id.block_cuisine -> mParent!!.editCuisine()
            R.id.category -> mParent!!.editCategory()
            R.id.more_names, R.id.show_additional_names -> {
                if (!(mNamesAdapter!!.areAdditionalLanguagesShown() && !validateNames()))
                    showAdditionalNames(!mNamesAdapter!!.areAdditionalLanguagesShown())
            }
            R.id.add_langs -> mParent!!.addLanguage()
            R.id.about_osm -> startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(Constants.Url.OSM_ABOUT)
                )
            )
            R.id.reset -> reset()
        }
    }

    private fun showAdditionalNames(show: Boolean) {
        mNamesAdapter!!.showAdditionalLanguages(show)
        refreshNamesCaption()
    }

    private fun refreshNamesCaption() {
        if (mNamesAdapter!!.namesCount <= mNamesAdapter!!.mandatoryNamesCount) setNamesArrow(0 /* arrowResourceId */) // bind arrow with empty resource (do not draw arrow)
        else if (mNamesAdapter!!.areAdditionalLanguagesShown()) setNamesArrow(R.drawable.ic_expand_less) else setNamesArrow(
            R.drawable.ic_expand_more
        )
        val showAddLanguage =
            mNamesAdapter!!.namesCount <= mNamesAdapter!!.mandatoryNamesCount ||
                    mNamesAdapter!!.areAdditionalLanguagesShown()
        UiUtils.showIf(showAddLanguage, mAddLanguage)
        UiUtils.showIf(!showAddLanguage, mMoreLanguages)
    }

    // Bind arrow in the top right corner of names caption with needed resource.
    private fun setNamesArrow(@DrawableRes arrowResourceId: Int) {
        if (arrowResourceId == 0) {
            mNamesCaption!!.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            return
        }
        mNamesCaption!!.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            Graphics.tint(activity!!, arrowResourceId, R.attr.iconTint),
            null
        )
    }

    private fun refreshResetButton() {
        if (mParent!!.addingNewObject()) {
            UiUtils.hide(mReset!!)
            return
        }
        if (Editor.nativeIsMapObjectUploaded()) {
            mReset!!.setText(R.string.editor_place_doesnt_exist)
            return
        }
        when (Editor.nativeGetMapObjectStatus()) {
            Editor.CREATED -> mReset!!.setText(R.string.editor_remove_place_button)
            Editor.MODIFIED -> mReset!!.setText(R.string.editor_reset_edits_button)
            Editor.UNTOUCHED -> mReset!!.setText(R.string.editor_place_doesnt_exist)
            Editor.DELETED -> throw IllegalStateException("Can't delete already deleted feature.")
            Editor.OBSOLETE -> throw IllegalStateException("Obsolete objects cannot be reverted.")
        }
    }

    private fun reset() {
        if (Editor.nativeIsMapObjectUploaded()) {
            placeDoesntExist()
            return
        }
        when (Editor.nativeGetMapObjectStatus()) {
            Editor.CREATED -> rollback(Editor.CREATED)
            Editor.MODIFIED -> rollback(Editor.MODIFIED)
            Editor.UNTOUCHED -> placeDoesntExist()
            Editor.DELETED -> throw IllegalStateException("Can't delete already deleted feature.")
            Editor.OBSOLETE -> throw IllegalStateException("Obsolete objects cannot be reverted.")
        }
    }

    private fun rollback(@Editor.FeatureStatus status: Int) {
        val title: Int
        val message: Int
        if (status == Editor.CREATED) {
            title = R.string.editor_remove_place_button
            message = R.string.editor_remove_place_message
        } else {
            title = R.string.editor_reset_edits_button
            message = R.string.editor_reset_edits_message
        }
        AlertDialog.Builder(activity!!).setTitle(message)
            .setPositiveButton(
                getString(title).toUpperCase()
            ) { dialog, which ->
                Editor.nativeRollbackMapObject()
                Framework.nativePokeSearchInViewport()
                mParent!!.onBackPressed()
            }
            .setNegativeButton(getString(R.string.cancel).toUpperCase(), null)
            .show()
    }

    private fun placeDoesntExist() {
        EditTextDialogFragment.show(
            getString(R.string.editor_place_doesnt_exist),
            "",
            getString(R.string.editor_comment_hint),
            getString(R.string.editor_report_problem_send_button),
            getString(R.string.cancel),
            this
        )
    }

    override val saveTextListener: OnTextSaveListener
        get() = object : OnTextSaveListener {
            override fun onSaveText(text: String) {
                Editor.nativePlaceDoesNotExist(text)
                mParent!!.onBackPressed()
            }
        }

    override val validator: EditTextDialogFragment.Validator
        get() = object : EditTextDialogFragment.Validator {
            override fun validate(activity: Activity, text: String?): Boolean {
                return !TextUtils.isEmpty(text)
            }
        }

    companion object {
        const val LAST_INDEX_OF_NAMES_ARRAY = "LastIndexOfNamesArray"
        private fun findInput(blockWithInput: View): EditText {
            return blockWithInput.findViewById<View>(R.id.input) as EditText
        }

        private fun findInputAndInitBlock(
            blockWithInput: View?, @DrawableRes icon: Int,
            hint: String
        ): EditText {
            (blockWithInput!!.findViewById<View>(R.id.icon) as ImageView).setImageResource(
                icon
            )
            val input =
                blockWithInput.findViewById<View>(R.id.custom_input) as TextInputLayout
            input.hint = hint
            return input.findViewById<View>(R.id.input) as EditText
        }
    }
}