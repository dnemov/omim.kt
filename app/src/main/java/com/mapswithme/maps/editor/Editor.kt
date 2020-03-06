package com.mapswithme.maps.editor

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.background.AppBackgroundTracker.OnTransitionListener
import com.mapswithme.maps.background.WorkerService.Companion.startActionUploadOsmChanges
import com.mapswithme.maps.editor.data.*
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Edits active(selected on the map) feature, which is represented as osm::EditableFeature in the core.
 */
object Editor {
    const val UNTOUCHED = 0
    const val DELETED = 1
    const val OBSOLETE = 2
    const val MODIFIED = 3
    const val CREATED = 4
    @JvmStatic private external fun nativeInit()
    @JvmStatic
    fun init(context: Context) {
        MwmApplication.backgroundTracker()?.addListener(OsmUploadListener(context))
    }

    @WorkerThread
    fun uploadChanges() {
        if (nativeHasSomethingToUpload() && OsmOAuth.isAuthorized) nativeUploadChanges(
            OsmOAuth.authToken,
            OsmOAuth.authSecret,
            BuildConfig.VERSION_NAME,
            BuildConfig.APPLICATION_ID
        )
    }

    @JvmStatic external fun nativeShouldShowEditPlace(): Boolean

    @JvmStatic external fun nativeShouldShowAddPlace(): Boolean

    @JvmStatic external fun nativeShouldShowAddBusiness(): Boolean
    @JvmStatic external fun nativeGetEditableFields(): IntArray
    @JvmStatic external fun nativeGetCategory(): String?
    @JvmStatic external fun nativeGetOpeningHours(): String?
    @JvmStatic external fun nativeSetOpeningHours(openingHours: String?)
    @JvmStatic external fun nativeGetPhone(): String?
    @JvmStatic external fun nativeSetPhone(phone: String?)
    @JvmStatic external fun nativeGetWebsite(): String?
    @JvmStatic external fun nativeSetWebsite(website: String?)
    @JvmStatic external fun nativeGetEmail(): String?
    @JvmStatic external fun nativeSetEmail(email: String?)
    @JvmStatic external fun nativeGetStars(): Int
    @JvmStatic external fun nativeSetStars(stars: String?)
    @JvmStatic external fun nativeGetOperator(): String?
    @JvmStatic external fun nativeSetOperator(operator: String?)
    @JvmStatic external fun nativeGetWikipedia(): String?
    @JvmStatic external fun nativeSetWikipedia(wikipedia: String?)
    @JvmStatic external fun nativeGetFlats(): String?
    @JvmStatic external fun nativeSetFlats(flats: String?)
    @JvmStatic external fun nativeGetBuildingLevels(): String?
    @JvmStatic external fun nativeSetBuildingLevels(levels: String?)
    @JvmStatic external fun nativeGetZipCode(): String?
    @JvmStatic external fun nativeSetZipCode(zipCode: String?)
    @JvmStatic external fun nativeHasWifi(): Boolean
    @JvmStatic external fun nativeSetHasWifi(hasWifi: Boolean): Boolean
    @JvmStatic external fun nativeIsAddressEditable(): Boolean
    @JvmStatic external fun nativeIsNameEditable(): Boolean
    @JvmStatic external fun nativeIsPointType(): Boolean
    @JvmStatic external fun nativeIsBuilding(): Boolean
    @JvmStatic external fun nativeGetNamesDataSource(needFakes: Boolean): NamesDataSource?
    @JvmStatic external fun nativeGetDefaultName(): String?
    @JvmStatic external fun nativeEnableNamesAdvancedMode()
    @JvmStatic external fun nativeSetNames(names: Array<LocalizedName>)
    @JvmStatic external fun nativeMakeLocalizedName(
        langCode: String?,
        name: String?
    ): LocalizedName

    @JvmStatic external fun nativeGetSupportedLanguages(): Array<Language>
    @JvmStatic external fun nativeGetStreet(): LocalizedStreet?
    @JvmStatic external fun nativeSetStreet(street: LocalizedStreet?)
    @JvmStatic external fun nativeGetNearbyStreets(): Array<LocalizedStreet>
    @JvmStatic external fun nativeGetHouseNumber(): String?
    @JvmStatic external fun nativeSetHouseNumber(houseNumber: String?)
    @JvmStatic external fun nativeIsHouseValid(houseNumber: String?): Boolean
    @JvmStatic external fun nativeIsLevelValid(level: String?): Boolean
    @JvmStatic external fun nativeIsFlatValid(flat: String?): Boolean
    @JvmStatic external fun nativeIsZipcodeValid(zipCode: String?): Boolean
    @JvmStatic external fun nativeIsPhoneValid(phone: String?): Boolean
    @JvmStatic external fun nativeIsWebsiteValid(site: String?): Boolean
    @JvmStatic external fun nativeIsEmailValid(email: String?): Boolean
    @JvmStatic external fun nativeIsNameValid(name: String?): Boolean
    @JvmStatic external fun nativeHasSomethingToUpload(): Boolean
    @WorkerThread
    @JvmStatic private external fun nativeUploadChanges(
        token: String?,
        secret: String?,
        appVersion: String,
        appId: String
    )

    /**
     * @return array [total edits count, uploaded edits count, last upload timestamp in seconds]
     */
    @Size(3)
    @JvmStatic external fun nativeGetStats(): LongArray

    @JvmStatic external fun nativeClearLocalEdits()
    /**
     * That method should be called, when user opens editor from place page, so that information in editor
     * could refresh.
     */

    @JvmStatic external fun nativeStartEdit()

    /**
     * @return true if feature was saved. False if some error occurred (eg. no space)
     */
    @JvmStatic external fun nativeSaveEditedFeature(): Boolean

    @JvmStatic external fun nativeGetAllCreatableFeatureTypes(lang: String): Array<String>
    @JvmStatic external fun nativeSearchCreatableFeatureTypes(
        query: String,
        lang: String
    ): Array<String>

    /**
     * Creates new object on the map. Places it in the center of current viewport.
     * [Framework.nativeIsDownloadedMapAtScreenCenter] should be called before
     * to check whether new feature can be created on the map.
     */
    fun createMapObject(category: FeatureCategory?) {
        nativeCreateMapObject(category!!.type)
    }

    @JvmStatic external fun nativeCreateMapObject(type: String)
    @JvmStatic external fun nativeCreateNote(text: String?)
    @JvmStatic external fun nativePlaceDoesNotExist(comment: String)
    @JvmStatic external fun nativeRollbackMapObject()
    /**
     * @return all cuisines keys.
     */
    @JvmStatic external fun nativeGetCuisines(): Array<String>?

    /**
     * @return selected cuisines keys.
     */
    @JvmStatic external fun nativeGetSelectedCuisines(): Array<String>?

    @JvmStatic external fun nativeTranslateCuisines(keys: Array<String>?): Array<String>?
    @JvmStatic external fun nativeSetSelectedCuisines(keys: Array<String>?)
    /**
     * @return properly formatted and appended cuisines string to display in UI.
     */
    @JvmStatic external fun nativeGetFormattedCuisine(): String?


    @JvmStatic external fun nativeGetMwmName(): String?

    @JvmStatic external fun nativeGetMwmVersion(): Long
    @FeatureStatus
    @JvmStatic external fun nativeGetMapObjectStatus(): Int

    @JvmStatic external fun nativeIsMapObjectUploaded(): Boolean
    // Should correspond to core osm::FeatureStatus.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        UNTOUCHED,
        DELETED,
        OBSOLETE,
        MODIFIED,
        CREATED
    )
    annotation class FeatureStatus

    private class OsmUploadListener internal constructor(context: Context) :
        OnTransitionListener {
        private val mContext: Context
        override fun onTransit(foreground: Boolean) {
            if (foreground) return
            startActionUploadOsmChanges(mContext)
        }

        init {
            mContext = context.applicationContext
        }
    }

    init {
        nativeInit()
    }
}