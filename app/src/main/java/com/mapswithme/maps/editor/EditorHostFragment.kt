package com.mapswithme.maps.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.maps.dialog.DialogUtils.showAlertDialog
import com.mapswithme.maps.editor.data.Language
import com.mapswithme.maps.editor.data.LocalizedName
import com.mapswithme.maps.editor.data.LocalizedStreet
import com.mapswithme.maps.intent.Factory.ShowDialogTask
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.Statistics
import java.util.*

class EditorHostFragment : BaseMwmToolbarFragment(), OnBackPressListener,
    View.OnClickListener, LanguagesFragment.Listener {
    private var mIsNewObject = false

    internal enum class Mode {
        MAP_OBJECT, OPENING_HOURS, STREET, CUISINE, LANGUAGE
    }

    private var mMode: Mode? = null
    /**
     * Count of names which should always be shown.
     */
    var mandatoryNamesCount = 0

    /**
     * Used in MultilanguageAdapter to show, select and remove items.
     */
    val names: List<LocalizedName>
        get() = sNames

    val namesAsArray: Array<LocalizedName>
        get() = sNames.toTypedArray()

    fun setNames(names: Array<LocalizedName>?) {
        sNames.clear()
        for (name in names!!) {
            addName(name)
        }
    }

    /**
     * Sets .name of an index item to name.
     */
    fun setName(name: String, index: Int) {
        sNames[index]!!.name = name
    }

    fun addName(name: LocalizedName) {
        sNames.add(name)
    }

    private fun fillNames(needFakes: Boolean) {
        val namesDataSource =
            Editor.nativeGetNamesDataSource(needFakes)
        setNames(namesDataSource!!.names)
        mandatoryNamesCount = namesDataSource.mandatoryNamesCount
        editMapObject()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editor_host, container, false)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbarController.toolbar.findViewById<View>(R.id.save)
            .setOnClickListener(this)
        toolbarController.toolbar
            .setNavigationOnClickListener { onBackPressed() }
        if (arguments != null) mIsNewObject =
            arguments!!.getBoolean(EditorActivity.Companion.EXTRA_NEW_OBJECT, false)
        toolbarController.setTitle(title)
        fillNames(true /* addFakes */)
    }

    @get:StringRes
    private val title: Int
        private get() = if (mIsNewObject) R.string.editor_add_place_title else R.string.editor_edit_place_title

    override fun onCreateToolbarController(root: View): ToolbarController {
        return object : SearchToolbarController(root, activity) {
            override fun onTextChanged(query: String) {
                val fragment =
                    childFragmentManager.findFragmentByTag(
                        CuisineFragment::class.java.name
                    )
                if (fragment != null) (fragment as CuisineFragment).setFilter(query)
            }
        }
    }

    override fun onBackPressed(): Boolean {
        when (mMode) {
            Mode.OPENING_HOURS, Mode.STREET, Mode.CUISINE, Mode.LANGUAGE -> editMapObject()
            else -> Utils.navigateToParent(activity)
        }
        return true
    }

    protected fun editMapObject(focusToLastName: Boolean = false /* focusToLastName */) {
        mMode = Mode.MAP_OBJECT
        (toolbarController as SearchToolbarController).showControls(false)
        toolbarController.setTitle(title)
        UiUtils.show(toolbarController.toolbar.findViewById(R.id.save))
        val args = Bundle()
        if (focusToLastName) args.putInt(
            EditorFragment.Companion.LAST_INDEX_OF_NAMES_ARRAY,
            sNames.size - 1
        )
        val editorFragment =
            instantiate(
                activity!!,
                EditorFragment::class.java.name,
                args
            )
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, editorFragment, EditorFragment::class.java.name)
            .commit()
    }

    fun editTimetable() {
        val args = Bundle()
        args.putString(
            TimetableContainerFragment.Companion.EXTRA_TIME,
            Editor.nativeGetOpeningHours()
        )
        editWithFragment(
            Mode.OPENING_HOURS,
            R.string.editor_time_title,
            args,
            TimetableContainerFragment::class.java,
            false
        )
    }

    fun editStreet() {
        editWithFragment(
            Mode.STREET,
            R.string.choose_street,
            null,
            StreetFragment::class.java,
            false
        )
    }

    fun editCuisine() {
        editWithFragment(
            Mode.CUISINE,
            R.string.select_cuisine,
            null,
            CuisineFragment::class.java,
            true
        )
    }

    fun addLanguage() {
        val args = Bundle()
        val languages =
            ArrayList<String>(sNames.size)
        for (name in sNames) languages.add(name!!.lang)
        args.putStringArrayList(LanguagesFragment.Companion.EXISTING_LOCALIZED_NAMES, languages)
        editWithFragment(
            Mode.LANGUAGE,
            R.string.choose_language,
            args,
            LanguagesFragment::class.java,
            false
        )
    }

    private fun editWithFragment(
        newMode: Mode, @StringRes toolbarTitle: Int,
        args: Bundle?,
        fragmentClass: Class<out Fragment>,
        showSearch: Boolean
    ) {
        if (!setEdits()) return
        mMode = newMode
        toolbarController.setTitle(toolbarTitle)
        (toolbarController as SearchToolbarController).showControls(showSearch)
        val fragment =
            instantiate(activity!!, fragmentClass.name, args)
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, fragmentClass.name)
            .commit()
    }

    fun editCategory() {
        if (!mIsNewObject) return
        val host: Activity? = activity
        host!!.finish()
        startActivity(Intent(host, FeatureCategoryActivity::class.java))
    }

    private fun setEdits(): Boolean {
        return (childFragmentManager.findFragmentByTag(EditorFragment::class.java.name) as EditorFragment?)!!.setEdits()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.save) {
            when (mMode) {
                Mode.OPENING_HOURS -> {
                    val timetables = (childFragmentManager.findFragmentByTag(
                        TimetableContainerFragment::class.java.name
                    ) as TimetableContainerFragment?)!!.timetable
                    if (OpeningHours.nativeIsTimetableStringValid(timetables)) {
                        Editor.nativeSetOpeningHours(timetables)
                        editMapObject()
                    } else { // TODO (yunikkk) correct translation
                        showMistakeDialog(R.string.editor_correct_mistake)
                    }
                }
                Mode.STREET -> setStreet(
                    (childFragmentManager.findFragmentByTag(
                        StreetFragment::class.java.name
                    ) as StreetFragment?)!!.street
                )
                Mode.CUISINE -> {
                    val cuisines =
                        (childFragmentManager.findFragmentByTag(
                            CuisineFragment::class.java.name
                        ) as CuisineFragment?)!!.cuisines
                    Editor.nativeSetSelectedCuisines(cuisines)
                    editMapObject()
                }
                Mode.LANGUAGE -> editMapObject()
                Mode.MAP_OBJECT -> {
                    if (!setEdits()) return
                    // Save object edits
                    if (MwmApplication.prefs()?.contains(NOOB_ALERT_SHOWN) != true) {
                        showNoobDialog()
                    } else {
                        saveNote()
                        saveMapObjectEdits()
                    }
                }
            }
        }
    }

    private fun saveMapObjectEdits() {
        if (Editor.nativeSaveEditedFeature()) {
            Statistics.INSTANCE.trackEditorSuccess(mIsNewObject)
            if (OsmOAuth.isAuthorized || !ConnectionState.isConnected) Utils.navigateToParent(
                activity
            ) else {
                val parent: Activity? = activity
                val intent = Intent(parent, MwmActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                intent.putExtra(
                    MwmActivity.EXTRA_TASK,
                    ShowDialogTask(AuthDialogFragment::class.java.name)
                )
                parent!!.startActivity(intent)
                if (parent is MwmActivity) parent.customOnNavigateUp() else parent.finish()
            }
        } else {
            Statistics.INSTANCE.trackEditorError(mIsNewObject)
            showAlertDialog(activity!!, R.string.downloader_no_space_title)
        }
    }

    private fun saveNote() {
        val tag = EditorFragment::class.java.name
        val fragment =
            childFragmentManager.findFragmentByTag(tag) as EditorFragment?
        val note = fragment!!.description
        if (!TextUtils.isEmpty(note)) Editor.nativeCreateNote(note)
    }

    private fun showMistakeDialog(@StringRes resId: Int) {
        AlertDialog.Builder(activity!!)
            .setMessage(resId)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showNoobDialog() {
        AlertDialog.Builder(activity!!)
            .setTitle(R.string.editor_share_to_all_dialog_title)
            .setMessage(
                getString(R.string.editor_share_to_all_dialog_message_1)
                        + " " + getString(R.string.editor_share_to_all_dialog_message_2)
            )
            .setPositiveButton(android.R.string.ok) { dlg, which ->
                MwmApplication.prefs()?.edit()
                    ?.putBoolean(NOOB_ALERT_SHOWN, true)
                    ?.apply()
                saveNote()
                saveMapObjectEdits()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun setStreet(street: LocalizedStreet?) {
        Editor.nativeSetStreet(street)
        editMapObject()
    }

    fun addingNewObject(): Boolean {
        return mIsNewObject
    }

    override fun onLanguageSelected(lang: Language) {
        var name: String? = ""
        if (lang.code == Language.Companion.DEFAULT_LANG_CODE) {
            fillNames(false /* addFakes */)
            name = Editor.nativeGetDefaultName()
            Editor.nativeEnableNamesAdvancedMode()
        }
        addName(Editor.nativeMakeLocalizedName(lang.code, name))
        editMapObject(true /* focusToLastName */)
    }

    companion object {
        /**
         * A list of localized names added by a user and those that were in metadata.
         */
        private val sNames: MutableList<LocalizedName> =
            ArrayList()
        private const val NOOB_ALERT_SHOWN = "Alert_for_noob_was_shown"
    }
}