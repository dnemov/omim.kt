package com.mapswithme.maps.bookmarks.data

import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import com.mapswithme.maps.PrivateVariables
import com.mapswithme.maps.base.DataChangedListener
import com.mapswithme.maps.base.Observable
import com.mapswithme.maps.bookmarks.data.BookmarkCategory.AccessRules
import com.mapswithme.maps.bookmarks.data.FilterStrategy.Private
import com.mapswithme.maps.bookmarks.data.Icon.BookmarkIconType
import com.mapswithme.maps.bookmarks.data.Icon.PredefinedColor
import com.mapswithme.maps.metrics.UserActionsLogger
import com.mapswithme.util.KeyValue
import com.mapswithme.util.UTM.UTMContentType
import com.mapswithme.util.UTM.UTMType
import com.mapswithme.util.statistics.Statistics
import java.io.File
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

@MainThread
enum class BookmarkManager {
    INSTANCE;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(CLOUD_BACKUP, CLOUD_RESTORE)
    annotation class SynchronizationType

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        CLOUD_SUCCESS,
        CLOUD_AUTH_ERROR,
        CLOUD_NETWORK_ERROR,
        CLOUD_DISK_ERROR,
        CLOUD_USER_INTERRUPTED,
        CLOUD_INVALID_CALL
    )
    annotation class SynchronizationResult

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        CLOUD_BACKUP_EXISTS,
        CLOUD_NO_BACKUP,
        CLOUD_NOT_ENOUGH_DISK_SPACE
    )
    annotation class RestoringRequestResult

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        SORT_BY_TYPE,
        SORT_BY_DISTANCE,
        SORT_BY_TIME
    )
    annotation class SortingType

    private val mCategoriesCoreDataProvider: BookmarkCategoriesDataProvider =
        CoreBookmarkCategoriesDataProvider()
    private var mCurrentDataProvider = mCategoriesCoreDataProvider
    val bookmarkCategoriesCache = BookmarkCategoriesCache()
    private val mListeners: MutableList<BookmarksLoadingListener> =
        ArrayList()
    private val mSortingListeners: MutableList<BookmarksSortingListener> =
        ArrayList()
    private val mConversionListeners: MutableList<KmlConversionListener> =
        ArrayList()
    private val mSharingListeners: MutableList<BookmarksSharingListener> =
        ArrayList()
    private val mCloudListeners: MutableList<BookmarksCloudListener> =
        ArrayList()
    private val mCatalogListeners: MutableList<BookmarksCatalogListener> =
        ArrayList()
    private val mCatalogPingListeners: MutableList<BookmarksCatalogPingListener> =
        ArrayList()
    private val mInvalidCategoriesListeners: MutableList<BookmarksInvalidCategoriesListener> =
        ArrayList()

    companion object {
        const val CLOUD_BACKUP = 0
        const val CLOUD_RESTORE = 1
        const val CLOUD_SUCCESS = 0
        const val CLOUD_AUTH_ERROR = 1
        const val CLOUD_NETWORK_ERROR = 2
        const val CLOUD_DISK_ERROR = 3
        const val CLOUD_USER_INTERRUPTED = 4
        const val CLOUD_INVALID_CALL = 5
        const val CLOUD_BACKUP_EXISTS = 0
        const val CLOUD_NO_BACKUP = 1
        const val CLOUD_NOT_ENOUGH_DISK_SPACE = 2
        const val SORT_BY_TYPE = 0
        const val SORT_BY_DISTANCE = 1
        const val SORT_BY_TIME = 2


        @JvmStatic external fun nativeGetBookmarkCategories(): Array<BookmarkCategory>

        @JvmStatic private external fun nativeGetCategoriesCount(): Int
        @JvmStatic private external fun nativeGetCategoryPositionById(catId: Long): Int
        @JvmStatic private external fun nativeGetCategoryIdByPosition(position: Int): Long
        @JvmStatic private external fun nativeGetBookmarksCount(catId: Long): Int
        @JvmStatic private external fun nativeGetTracksCount(catId: Long): Int
        @JvmStatic private external fun nativeUpdateBookmarkPlacePage(bmkId: Long): Bookmark?
        @JvmStatic private external fun nativeGetBookmarkInfo(bmkId: Long): BookmarkInfo?
        @JvmStatic private external fun nativeGetBookmarkIdByPosition(
            catId: Long,
            position: Int
        ): Long

        @JvmStatic private external fun nativeGetTrack(
            trackId: Long,
            trackClazz: Class<Track>
        ): Track

        @JvmStatic private external fun nativeGetTrackIdByPosition(catId: Long, position: Int): Long
        @JvmStatic private external fun nativeIsVisible(catId: Long): Boolean
        @JvmStatic private external fun nativeSetVisibility(
            catId: Long,
            visible: Boolean
        )

        @JvmStatic private external fun nativeSetCategoryName(catId: Long, n: String)
        @JvmStatic private external fun nativeSetCategoryDescription(
            catId: Long,
            desc: String
        )

        @JvmStatic private external fun nativeSetCategoryTags(
            catId: Long,
            tagsIds: Array<String?>
        )

        @JvmStatic external fun nativeSetCategoryAccessRules(
            catId: Long,
            accessRules: Int
        )

        @JvmStatic external fun nativeSetCategoryCustomProperty(
            catId: Long,
            key: String,
            value: String
        )

        @JvmStatic external fun nativeDeleteCategory(catId: Long): Boolean
        @JvmStatic external fun nativeDeleteTrack(trackId: Long)
        @JvmStatic external fun nativeDeleteBookmark(bmkId: Long)
        /**
         * @return category Id
         */
        @JvmStatic external fun nativeCreateCategory(name: String): Long

        @JvmStatic external fun nativeShowBookmarkOnMap(bmkId: Long)
        @JvmStatic external fun nativeShowBookmarkCategoryOnMap(catId: Long)
        @JvmStatic external fun nativeAddBookmarkToLastEditedCategory(
            lat: Double,
            lon: Double
        ): Bookmark?

        @JvmStatic external fun nativeGetLastEditedCategory(): Long
        @PredefinedColor
        @JvmStatic external fun nativeGetLastEditedColor(): Int

        @JvmStatic external fun nativeSetCloudEnabled(enabled: Boolean)
        @JvmStatic external fun nativeIsCloudEnabled(): Boolean
        @JvmStatic external fun nativeGetLastSynchronizationTimestampInMs(): Long
        @JvmStatic external fun nativeHasLastSortingType(catId: Long): Boolean
        @SortingType
        @JvmStatic external fun nativeGetLastSortingType(catId: Long): Int

        @JvmStatic external fun nativeSetLastSortingType(catId: Long, @SortingType sortingType: Int)
        @JvmStatic external fun nativeResetLastSortingType(catId: Long)
        @SortingType
        @JvmStatic external fun nativeGetAvailableSortingTypes(
            catId: Long,
            hasMyPosition: Boolean
        ): IntArray

        @JvmStatic external fun nativeGetSortedCategory(
            catId: Long, @SortingType sortingType: Int,
            hasMyPosition: Boolean, lat: Double, lon: Double,
            timestamp: Long
        ): Boolean

        @JvmField
        val ICONS: MutableList<Icon> =
            ArrayList()

        @JvmStatic
        fun loadBookmarks() {
            nativeLoadBookmarks()
        }

        @JvmStatic
        private external fun nativeLoadBookmarks()
        @JvmStatic external fun nativeLoadKmzFile(
            path: String,
            isTemporaryFile: Boolean
        )

        @JvmStatic
        private external fun nativeIsAsyncBookmarksLoadingInProgress(): Boolean

        @JvmStatic
        private external fun nativeIsUsedCategoryName(name: String): Boolean
        @JvmStatic external fun nativeIsEditableBookmark(bmkId: Long): Boolean
        @JvmStatic external fun nativeIsEditableTrack(trackId: Long): Boolean
        @JvmStatic external fun nativeIsEditableCategory(catId: Long): Boolean
        @JvmStatic external fun nativeIsSearchAllowed(catId: Long): Boolean
        @JvmStatic external fun nativePrepareForSearch(catId: Long)
        @JvmStatic external fun nativeAreAllCategoriesVisible(type: Int): Boolean
        @JvmStatic external fun nativeAreAllCategoriesInvisible(type: Int): Boolean
        @JvmStatic external fun nativeSetAllCategoriesVisibility(
            visible: Boolean,
            type: Int
        )

        @JvmStatic external fun nativeGetKmlFilesCountForConversion(): Int
        @JvmStatic external fun nativeConvertAllKmlFiles()
        @JvmStatic external fun nativePrepareFileForSharing(catId: Long)
        @JvmStatic external fun nativeIsCategoryEmpty(catId: Long): Boolean
        @JvmStatic external fun nativeRequestRestoring()
        @JvmStatic external fun nativeApplyRestoring()
        @JvmStatic external fun nativeCancelRestoring()
        @JvmStatic external fun nativeSetNotificationsEnabled(enabled: Boolean)
        @JvmStatic external fun nativeAreNotificationsEnabled(): Boolean
        @JvmStatic external fun nativeImportFromCatalog(
            serverId: String,
            filePath: String
        )

        @JvmStatic external fun nativeUploadToCatalog(
            accessRules: Int,
            catId: Long
        )

        @JvmStatic external fun nativeGetCatalogDeeplink(catId: Long): String
        @JvmStatic external fun nativeGetCatalogPublicLink(catId: Long): String
        @JvmStatic external fun nativeGetCatalogDownloadUrl(serverId: String): String
        @JvmStatic external fun nativeGetWebEditorUrl(serverId: String): String
        @JvmStatic external fun nativeGetCatalogFrontendUrl(@UTMType utm: Int): String
        @JvmStatic external fun nativeGetCatalogHeaders(): Array<KeyValue>
        @JvmStatic external fun nativeInjectCatalogUTMContent(
            url: String,
            @UTMContentType content: Int
        ): String

        @JvmStatic external fun nativeIsCategoryFromCatalog(catId: Long): Boolean
        @JvmStatic external fun nativeRequestCatalogTags()
        @JvmStatic external fun nativeRequestCatalogCustomProperties()
        @JvmStatic external fun nativePingBookmarkCatalog()
        @JvmStatic external fun nativeCheckInvalidCategories()
        @JvmStatic external fun nativeDeleteInvalidCategories()
        @JvmStatic external fun nativeResetInvalidCategories()
        @JvmStatic external fun nativeGuidesIds(): String
        @JvmStatic external fun nativeIsGuide(accessRulesIndex: Int): Boolean
        @JvmStatic external fun nativeGetBookmarkName(@IntRange(from = 0) bookmarkId: Long): String
        @JvmStatic external fun nativeGetBookmarkFeatureType(@IntRange(from = 0) bookmarkId: Long): String
        @JvmStatic external fun nativeGetBookmarkXY(@IntRange(from = 0) bookmarkId: Long): ParcelablePointD
        @PredefinedColor
        @JvmStatic external fun nativeGetBookmarkColor(@IntRange(from = 0) bookmarkId: Long): Int

        @BookmarkIconType
        @JvmStatic external fun nativeGetBookmarkIcon(@IntRange(from = 0) bookmarkId: Long): Int

        @JvmStatic external fun nativeGetBookmarkDescription(@IntRange(from = 0) bookmarkId: Long): String
        @JvmStatic external fun nativeGetBookmarkScale(@IntRange(from = 0) bookmarkId: Long): Double
        @JvmStatic external fun nativeEncode2Ge0Url(
            @IntRange(from = 0) bookmarkId: Long,
            addName: Boolean
        ): String

        @JvmStatic external fun nativeSetBookmarkParams(
            @IntRange(from = 0) bookmarkId: Long,
            name: String,
            @PredefinedColor color: Int,
            descr: String
        )

        @JvmStatic external fun nativeChangeBookmarkCategory(
            @IntRange(from = 0) oldCatId: Long,
            @IntRange(from = 0) newCatId: Long,
            @IntRange(from = 0) bookmarkId: Long
        )

        @JvmStatic external fun nativeGetBookmarkAddress(@IntRange(from = 0) bookmarkId: Long): String

        init {
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_RED,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_PINK,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_PURPLE,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_DEEPPURPLE,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_BLUE,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_LIGHTBLUE,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_CYAN,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_TEAL,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_GREEN,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_LIME,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_YELLOW,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_ORANGE,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_DEEPORANGE,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_BROWN,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_GRAY,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
            ICONS.add(
                Icon(
                    Icon.PREDEFINED_COLOR_BLUEGRAY,
                    Icon.BOOKMARK_ICON_TYPE_NONE
                )
            )
        }
    }

    fun toggleCategoryVisibility(catId: Long) {
        val isVisible = isVisible(catId)
        setVisibility(catId, !isVisible)
    }

    fun addNewBookmark(lat: Double, lon: Double): Bookmark? {
        val bookmark = nativeAddBookmarkToLastEditedCategory(lat, lon)
        if (bookmark != null) {
            UserActionsLogger.logAddToBookmarkEvent()
            Statistics.INSTANCE.trackBookmarkCreated()
        }
        return bookmark
    }

    fun addLoadingListener(listener: BookmarksLoadingListener) {
        mListeners.add(listener)
    }

    fun removeLoadingListener(listener: BookmarksLoadingListener) {
        mListeners.remove(listener)
    }

    fun addSortingListener(listener: BookmarksSortingListener) {
        mSortingListeners.add(listener)
    }

    fun removeSortingListener(listener: BookmarksSortingListener) {
        mSortingListeners.remove(listener)
    }

    fun addKmlConversionListener(listener: KmlConversionListener) {
        mConversionListeners.add(listener)
    }

    fun removeKmlConversionListener(listener: KmlConversionListener) {
        mConversionListeners.remove(listener)
    }

    fun addSharingListener(listener: BookmarksSharingListener) {
        mSharingListeners.add(listener)
    }

    fun removeSharingListener(listener: BookmarksSharingListener) {
        mSharingListeners.remove(listener)
    }

    fun addCloudListener(listener: BookmarksCloudListener) {
        mCloudListeners.add(listener)
    }

    fun removeCloudListener(listener: BookmarksCloudListener) {
        mCloudListeners.remove(listener)
    }

    fun addCatalogListener(listener: BookmarksCatalogListener) {
        mCatalogListeners.add(listener)
    }

    fun removeCatalogListener(listener: BookmarksCatalogListener) {
        mCatalogListeners.remove(listener)
    }

    fun addInvalidCategoriesListener(listener: BookmarksInvalidCategoriesListener) {
        mInvalidCategoriesListeners.add(listener)
    }

    fun removeInvalidCategoriesListener(listener: BookmarksInvalidCategoriesListener) {
        mInvalidCategoriesListeners.remove(listener)
    }

    fun addCatalogPingListener(listener: BookmarksCatalogPingListener) {
        mCatalogPingListeners.add(listener)
    }

    fun removeCatalogPingListener(listener: BookmarksCatalogPingListener) {
        mCatalogPingListeners.remove(listener)
    }

    // Called from JNI.
    @MainThread
    fun onBookmarksChanged() {
        updateCache()
    }

    @MainThread
    fun onBookmarksLoadingStarted() {
        for (listener in mListeners) listener.onBookmarksLoadingStarted()
    }

    // Called from JNI.
    @MainThread
    fun onBookmarksLoadingFinished() {
        updateCache()
        mCurrentDataProvider = CacheBookmarkCategoriesDataProvider()
        for (listener in mListeners) listener.onBookmarksLoadingFinished()
    }

    // Called from JNI.
    @MainThread
    fun onBookmarksSortingCompleted(
        sortedBlocks: Array<SortedBlock>,
        timestamp: Long
    ) {
        for (listener in mSortingListeners) listener.onBookmarksSortingCompleted(
            sortedBlocks,
            timestamp
        )
    }

    // Called from JNI.
    @MainThread
    fun onBookmarksSortingCancelled(timestamp: Long) {
        for (listener in mSortingListeners) listener.onBookmarksSortingCancelled(
            timestamp
        )
    }

    // Called from JNI.
    @MainThread
    fun onBookmarksFileLoaded(
        success: Boolean, fileName: String,
        isTemporaryFile: Boolean
    ) { // Android could create temporary file with bookmarks in some cases (KML/KMZ file is a blob
// in the intent, so we have to create a temporary file on the disk). Here we can delete it.
        if (isTemporaryFile) {
            val tmpFile = File(fileName)
            tmpFile.delete()
        }
        for (listener in mListeners) listener.onBookmarksFileLoaded(
            success
        )
    }

    // Called from JNI.
    @MainThread
    fun onFinishKmlConversion(success: Boolean) {
        for (listener in mConversionListeners) listener.onFinishKmlConversion(
            success
        )
    }

    // Called from JNI.
    @MainThread
    fun onPreparedFileForSharing(result: BookmarkSharingResult) {
        for (listener in mSharingListeners) listener.onPreparedFileForSharing(
            result
        )
    }

    // Called from JNI.
    @MainThread
    fun onSynchronizationStarted(@SynchronizationType type: Int) {
        for (listener in mCloudListeners) listener.onSynchronizationStarted(
            type
        )
    }

    // Called from JNI.
    @MainThread
    fun onSynchronizationFinished(
        @SynchronizationType type: Int,
        @SynchronizationResult result: Int,
        errorString: String
    ) {
        for (listener in mCloudListeners) listener.onSynchronizationFinished(
            type,
            result,
            errorString
        )
    }

    // Called from JNI.
    @MainThread
    fun onRestoreRequested(
        @RestoringRequestResult result: Int, deviceName: String,
        backupTimestampInMs: Long
    ) {
        for (listener in mCloudListeners) listener.onRestoreRequested(
            result,
            deviceName,
            backupTimestampInMs
        )
    }

    // Called from JNI.
    @MainThread
    fun onRestoredFilesPrepared() {
        for (listener in mCloudListeners) listener.onRestoredFilesPrepared()
    }

    // Called from JNI.
    @MainThread
    fun onImportStarted(id: String) {
        for (listener in mCatalogListeners) listener.onImportStarted(id)
    }

    // Called from JNI.
    @MainThread
    fun onImportFinished(
        id: String,
        catId: Long,
        successful: Boolean
    ) {
        if (successful) Statistics.INSTANCE.trackPurchaseProductDelivered(
            id,
            PrivateVariables.bookmarksVendor()
        )
        for (listener in mCatalogListeners) listener.onImportFinished(
            id,
            catId,
            successful
        )
    }

    // Called from JNI.
    @MainThread
    fun onTagsReceived(
        successful: Boolean, tagsGroups: Array<CatalogTagsGroup>,
        maxTagsCount: Int
    ) {
        val unmodifiableData =
            Collections.unmodifiableList(Arrays.asList(*tagsGroups))
        for (listener in mCatalogListeners) {
            listener.onTagsReceived(successful, unmodifiableData, maxTagsCount)
        }
    }

    // Called from JNI.
    @MainThread
    fun onCustomPropertiesReceived(
        successful: Boolean,
        properties: Array<CatalogCustomProperty>
    ) {
        val unmodifiableProperties =
            Collections.unmodifiableList(Arrays.asList(*properties))
        for (listener in mCatalogListeners) listener.onCustomPropertiesReceived(
            successful,
            unmodifiableProperties
        )
    }

    // Called from JNI.
    @MainThread
    fun onUploadStarted(originCategoryId: Long) {
        for (listener in mCatalogListeners) listener.onUploadStarted(
            originCategoryId
        )
    }

    // Called from JNI.
    @MainThread
    fun onUploadFinished(
        index: Int, description: String,
        originCategoryId: Long, resultCategoryId: Long
    ) {
        val result =
            UploadResult.values()[index]
        for (listener in mCatalogListeners) {
            listener.onUploadFinished(result, description, originCategoryId, resultCategoryId)
        }
    }

    // Called from JNI.
    @MainThread
    fun onPingFinished(isServiceAvailable: Boolean) {
        for (listener in mCatalogPingListeners) listener.onPingFinished(
            isServiceAvailable
        )
    }

    // Called from JNI.
    @MainThread
    fun onCheckInvalidCategories(hasInvalidCategories: Boolean) {
        for (listener in mInvalidCategoriesListeners) listener.onCheckInvalidCategories(
            hasInvalidCategories
        )
    }

    fun isVisible(catId: Long): Boolean {
        return nativeIsVisible(catId)
    }

    fun setVisibility(catId: Long, visible: Boolean) {
        nativeSetVisibility(catId, visible)
    }

    fun setCategoryName(catId: Long, name: String) {
        nativeSetCategoryName(catId, name)
    }

    fun setCategoryDescription(id: Long, categoryDesc: String) {
        nativeSetCategoryDescription(id, categoryDesc)
    }

    fun setCategoryTags(
        category: BookmarkCategory,
        tags: List<CatalogTag>
    ) {
        val ids = arrayOfNulls<String>(tags.size)
        for (i in tags.indices) {
            ids[i] = tags[i].id
        }
        nativeSetCategoryTags(category.id, ids)
    }

    fun setCategoryProperties(
        category: BookmarkCategory,
        properties: List<CatalogPropertyOptionAndKey>
    ) {
        for (each in properties) {
            nativeSetCategoryCustomProperty(category.id, each.key, each.option.value)
        }
    }

    fun setAccessRules(id: Long, rules: AccessRules) {
        nativeSetCategoryAccessRules(id, rules.ordinal)
    }

    fun uploadToCatalog(rules: AccessRules, category: BookmarkCategory) {
        nativeUploadToCatalog(rules.ordinal, category.id)
    }

    fun updateBookmarkPlacePage(bmkId: Long): Bookmark? {
        return nativeUpdateBookmarkPlacePage(bmkId)
    }

    fun getBookmarkInfo(bmkId: Long): BookmarkInfo? {
        return nativeGetBookmarkInfo(bmkId)
    }

    fun getBookmarkIdByPosition(catId: Long, positionInCategory: Int): Long {
        return nativeGetBookmarkIdByPosition(catId, positionInCategory)
    }

    fun getTrack(trackId: Long): Track {
        return nativeGetTrack(trackId, Track::class.java)
    }

    fun getTrackIdByPosition(catId: Long, positionInCategory: Int): Long {
        return nativeGetTrackIdByPosition(catId, positionInCategory)
    }

    fun deleteCategory(catId: Long) {
        nativeDeleteCategory(catId)
    }

    fun deleteTrack(trackId: Long) {
        nativeDeleteTrack(trackId)
    }

    fun deleteBookmark(bmkId: Long) {
        nativeDeleteBookmark(bmkId)
    }

    fun createCategory(name: String): Long {
        return nativeCreateCategory(name)
    }

    fun showBookmarkOnMap(bmkId: Long) {
        nativeShowBookmarkOnMap(bmkId)
    }

    fun showBookmarkCategoryOnMap(catId: Long) {
        nativeShowBookmarkCategoryOnMap(catId)
    }

    val lastEditedCategory: Long
        get() = nativeGetLastEditedCategory()

    @get:PredefinedColor
    val lastEditedColor: Int
        get() = nativeGetLastEditedColor()

    var isCloudEnabled: Boolean
        get() = nativeIsCloudEnabled()
        set(enabled) {
            nativeSetCloudEnabled(enabled)
        }

    val lastSynchronizationTimestampInMs: Long
        get() = nativeGetLastSynchronizationTimestampInMs()

    fun loadKmzFile(path: String, isTemporaryFile: Boolean) {
        nativeLoadKmzFile(path, isTemporaryFile)
    }

    val isAsyncBookmarksLoadingInProgress: Boolean
        get() = nativeIsAsyncBookmarksLoadingInProgress()

    val downloadedCategoriesSnapshot: AbstractCategoriesSnapshot.Default
        get() {
            val items = mCurrentDataProvider.categories
            return AbstractCategoriesSnapshot.Default(
                items,
                FilterStrategy.Downloaded()
            )
        }

    val ownedCategoriesSnapshot: AbstractCategoriesSnapshot.Default
        get() {
            val items = mCurrentDataProvider.categories
            return AbstractCategoriesSnapshot.Default(
                items,
                Private()
            )
        }

    val allCategoriesSnapshot: AbstractCategoriesSnapshot.Default
        get() {
            val items = mCurrentDataProvider.categories
            return AbstractCategoriesSnapshot.Default(
                items,
                FilterStrategy.All()
            )
        }

    fun getCategoriesSnapshot(strategy: FilterStrategy): AbstractCategoriesSnapshot.Default {
        val items = mCurrentDataProvider.categories
        return AbstractCategoriesSnapshot.Default(
            items,
            strategy
        )
    }

    private fun updateCache() {
        bookmarkCategoriesCache.update(mCategoriesCoreDataProvider.categories)
    }

    fun addCategoriesUpdatesListener(listener: DataChangedListener<*>) {
        bookmarkCategoriesCache.registerListener(listener)
    }

    fun removeCategoriesUpdatesListener(listener: DataChangedListener<*>) {
        bookmarkCategoriesCache.unregisterListener(listener)
    }

    fun getCategoryById(categoryId: Long): BookmarkCategory {
        return mCurrentDataProvider.getCategoryById(categoryId)
    }

    fun isUsedCategoryName(name: String): Boolean {
        return nativeIsUsedCategoryName(name)
    }

    fun isEditableBookmark(bmkId: Long): Boolean {
        return nativeIsEditableBookmark(bmkId)
    }

    fun isEditableTrack(trackId: Long): Boolean {
        return nativeIsEditableTrack(trackId)
    }

    fun isEditableCategory(catId: Long): Boolean {
        return nativeIsEditableCategory(catId)
    }

    fun isSearchAllowed(catId: Long): Boolean {
        return nativeIsSearchAllowed(catId)
    }

    fun prepareForSearch(catId: Long) {
        nativePrepareForSearch(catId)
    }

    fun areAllCategoriesVisible(type: BookmarkCategory.Type): Boolean {
        return nativeAreAllCategoriesVisible(type.ordinal)
    }

    fun areAllCategoriesInvisible(type: BookmarkCategory.Type): Boolean {
        return nativeAreAllCategoriesInvisible(type.ordinal)
    }

    fun areAllCatalogCategoriesInvisible(): Boolean {
        return areAllCategoriesInvisible(BookmarkCategory.Type.DOWNLOADED)
    }

    fun areAllOwnedCategoriesInvisible(): Boolean {
        return areAllCategoriesInvisible(BookmarkCategory.Type.PRIVATE)
    }

    fun setAllCategoriesVisibility(
        visible: Boolean,
        type: BookmarkCategory.Type
    ) {
        nativeSetAllCategoriesVisibility(visible, type.ordinal)
    }

    val kmlFilesCountForConversion: Int
        get() = nativeGetKmlFilesCountForConversion()

    fun convertAllKmlFiles() {
        nativeConvertAllKmlFiles()
    }

    fun prepareFileForSharing(catId: Long) {
        nativePrepareFileForSharing(catId)
    }

    fun isCategoryEmpty(catId: Long): Boolean {
        return nativeIsCategoryEmpty(catId)
    }

    fun prepareCategoryForSharing(catId: Long) {
        nativePrepareFileForSharing(catId)
    }

    fun requestRestoring() {
        nativeRequestRestoring()
    }

    fun applyRestoring() {
        nativeApplyRestoring()
    }

    fun cancelRestoring() {
        nativeCancelRestoring()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        nativeSetNotificationsEnabled(enabled)
    }

    fun areNotificationsEnabled(): Boolean {
        return nativeAreNotificationsEnabled()
    }

    fun importFromCatalog(serverId: String, filePath: String) {
        nativeImportFromCatalog(serverId, filePath)
    }

    fun uploadRoutes(accessRules: Int, bookmarkCategory: BookmarkCategory) {
        nativeUploadToCatalog(accessRules, bookmarkCategory.id)
    }

    fun getCatalogDeeplink(catId: Long): String {
        return nativeGetCatalogDeeplink(catId)
    }

    fun getCatalogPublicLink(catId: Long): String {
        return nativeGetCatalogPublicLink(catId)
    }

    fun getCatalogDownloadUrl(serverId: String): String {
        return nativeGetCatalogDownloadUrl(serverId)
    }

    fun getWebEditorUrl(serverId: String): String {
        return nativeGetWebEditorUrl(serverId)
    }

    fun getCatalogFrontendUrl(@UTMType utm: Int): String {
        return nativeGetCatalogFrontendUrl(utm)
    }

    val catalogHeaders: Array<KeyValue>
        get() = nativeGetCatalogHeaders()

    fun injectCatalogUTMContent(url: String, @UTMContentType content: Int): String {
        return nativeInjectCatalogUTMContent(url, content)
    }

    val guidesIds: String
        get() = nativeGuidesIds()

    fun isGuide(category: BookmarkCategory): Boolean {
        return category.isFromCatalog && nativeIsGuide(category.accessRules.ordinal)
    }

    fun requestRouteTags() {
        nativeRequestCatalogTags()
    }

    fun requestCustomProperties() {
        nativeRequestCatalogCustomProperties()
    }

    fun pingBookmarkCatalog() {
        nativePingBookmarkCatalog()
    }

    fun checkInvalidCategories() {
        nativeCheckInvalidCategories()
    }

    fun deleteInvalidCategories() {
        nativeDeleteInvalidCategories()
    }

    fun resetInvalidCategories() {
        nativeResetInvalidCategories()
    }

    fun isCategoryFromCatalog(catId: Long): Boolean {
        return nativeIsCategoryFromCatalog(catId)
    }

    fun hasLastSortingType(catId: Long): Boolean {
        return nativeHasLastSortingType(catId)
    }

    @SortingType
    fun getLastSortingType(catId: Long): Int {
        return nativeGetLastSortingType(catId)
    }

    fun setLastSortingType(catId: Long, @SortingType sortingType: Int) {
        nativeSetLastSortingType(catId, sortingType)
    }

    fun resetLastSortingType(catId: Long) {
        nativeResetLastSortingType(catId)
    }

    @SortingType
    fun getAvailableSortingTypes(catId: Long, hasMyPosition: Boolean): IntArray {
        return nativeGetAvailableSortingTypes(catId, hasMyPosition)
    }

    fun getSortedCategory(
        catId: Long, @SortingType sortingType: Int,
        hasMyPosition: Boolean, lat: Double, lon: Double,
        timestamp: Long
    ) {
        nativeGetSortedCategory(catId, sortingType, hasMyPosition, lat, lon, timestamp)
    }

    fun getBookmarkName(@IntRange(from = 0) bookmarkId: Long): String {
        return nativeGetBookmarkName(bookmarkId)
    }

    fun getBookmarkFeatureType(@IntRange(from = 0) bookmarkId: Long): String {
        return nativeGetBookmarkFeatureType(bookmarkId)
    }

    fun getBookmarkXY(@IntRange(from = 0) bookmarkId: Long): ParcelablePointD {
        return nativeGetBookmarkXY(bookmarkId)
    }

    @PredefinedColor
    fun getBookmarkColor(@IntRange(from = 0) bookmarkId: Long): Int {
        return nativeGetBookmarkColor(bookmarkId)
    }

    @BookmarkIconType
    fun getBookmarkIcon(@IntRange(from = 0) bookmarkId: Long): Int {
        return nativeGetBookmarkIcon(bookmarkId)
    }

    fun getBookmarkDescription(@IntRange(from = 0) bookmarkId: Long): String {
        return nativeGetBookmarkDescription(bookmarkId)
    }

    fun getBookmarkScale(@IntRange(from = 0) bookmarkId: Long): Double {
        return nativeGetBookmarkScale(bookmarkId)
    }

    fun encode2Ge0Url(@IntRange(from = 0) bookmarkId: Long, addName: Boolean): String {
        return nativeEncode2Ge0Url(bookmarkId, addName)
    }

    fun setBookmarkParams(
        @IntRange(from = 0) bookmarkId: Long, name: String,
        @PredefinedColor color: Int, descr: String
    ) {
        nativeSetBookmarkParams(bookmarkId, name, color, descr)
    }

    fun changeBookmarkCategory(
        @IntRange(from = 0) oldCatId: Long,
        @IntRange(from = 0) newCatId: Long,
        @IntRange(from = 0) bookmarkId: Long
    ) {
        nativeChangeBookmarkCategory(oldCatId, newCatId, bookmarkId)
    }

    fun getBookmarkAddress(@IntRange(from = 0) bookmarkId: Long): String {
        return nativeGetBookmarkAddress(bookmarkId)
    }

    fun notifyCategoryChanging(
        bookmarkInfo: BookmarkInfo,
        @IntRange(from = 0) catId: Long
    ) {
        if (catId == bookmarkInfo.categoryId) return
        changeBookmarkCategory(bookmarkInfo.categoryId, catId, bookmarkInfo.bookmarkId)
    }

    fun notifyCategoryChanging(bookmark: Bookmark, @IntRange(from = 0) catId: Long) {
        if (catId == bookmark.categoryId) return
        changeBookmarkCategory(bookmark.categoryId, catId, bookmark.bookmarkId)
    }

    fun notifyParametersUpdating(
        bookmarkInfo: BookmarkInfo, name: String,
        icon: Icon?, description: String
    ) {
        var icon = icon
        if (icon == null) icon = bookmarkInfo.icon
        if (name != bookmarkInfo.name || !icon.equals(bookmarkInfo.icon) ||
            description != getBookmarkDescription(bookmarkInfo.bookmarkId)
        ) {
            setBookmarkParams(bookmarkInfo.bookmarkId, name, icon!!.color, description)
        }
    }

    fun notifyParametersUpdating(
        bookmark: Bookmark, name: String,
        icon: Icon?, description: String
    ) {
        var icon = icon
        if (icon == null) icon = bookmark.icon
        if (name != bookmark.name || !icon!!.equals(bookmark.icon) ||
            description != getBookmarkDescription(bookmark.bookmarkId)
        ) {
            setBookmarkParams(
                bookmark.bookmarkId, name,
                icon?.color ?: lastEditedColor, description
            )
        }
    }



    interface BookmarksLoadingListener {
        fun onBookmarksLoadingStarted()
        fun onBookmarksLoadingFinished()
        fun onBookmarksFileLoaded(success: Boolean)
    }

    interface BookmarksSortingListener {
        fun onBookmarksSortingCompleted(
            sortedBlocks: Array<SortedBlock>,
            timestamp: Long
        )

        fun onBookmarksSortingCancelled(timestamp: Long)
    }

    interface KmlConversionListener {
        fun onFinishKmlConversion(success: Boolean)
    }

    interface BookmarksSharingListener {
        fun onPreparedFileForSharing(result: BookmarkSharingResult)
    }

    interface BookmarksCloudListener {
        /**
         * The method is called when the synchronization started.
         *
         * @param type determines type of synchronization (backup or restoring).
         */
        fun onSynchronizationStarted(@SynchronizationType type: Int)

        /**
         * The method is called when the synchronization finished.
         *
         * @param type determines type of synchronization (backup or restoring).
         * @param result is one of possible results of the synchronization.
         * @param errorString contains detailed description in case of unsuccessful completion.
         */
        fun onSynchronizationFinished(
            @SynchronizationType type: Int,
            @SynchronizationResult result: Int,
            errorString: String
        )

        /**
         * The method is called after restoring request.
         *
         * @param result By result you can determine if the restoring is possible.
         * @param deviceName The name of device which was the source of the backup.
         * @param backupTimestampInMs contains timestamp of the backup on the server (in milliseconds).
         */
        fun onRestoreRequested(
            @RestoringRequestResult result: Int, deviceName: String,
            backupTimestampInMs: Long
        )

        /**
         * Restored bookmark files are prepared to substitute for the current ones.
         * After this callback any cached bookmarks data become invalid. Also after this
         * callback the restoring process can not be cancelled.
         */
        fun onRestoredFilesPrepared()
    }

    interface BookmarksCatalogPingListener {
        fun onPingFinished(isServiceAvailable: Boolean)
    }

    interface BookmarksInvalidCategoriesListener {
        fun onCheckInvalidCategories(hasInvalidCategories: Boolean)
    }

    interface BookmarksCatalogListener {
        /**
         * The method is called when the importing of a file from the catalog is started.
         *
         * @param serverId is server identifier of the file.
         */
        fun onImportStarted(serverId: String)

        /**
         * The method is called when the importing of a file from the catalog is finished.
         *
         * @param serverId is server identifier of the file.
         * @param catId is client identifier of the created bookmarks category.
         * @param successful is result of the importing.
         */
        fun onImportFinished(
            serverId: String,
            catId: Long,
            successful: Boolean
        )

        /**
         * The method is called when the tags were received from the server.
         * @param successful is the result of the receiving.
         * @param tagsGroups is the tags collection.
         */
        fun onTagsReceived(
            successful: Boolean,
            tagsGroups: List<CatalogTagsGroup>,
            tagsLimit: Int
        )

        /**
         * The method is called when the custom properties were received from the server.
         * @param successful is the result of the receiving.
         * @param properties is the properties collection.
         */
        fun onCustomPropertiesReceived(
            successful: Boolean,
            properties: List<CatalogCustomProperty>
        )

        /**
         * The method is called when the uploading to the catalog is started.
         *
         * @param originCategoryId is identifier of the uploading bookmarks category.
         */
        fun onUploadStarted(originCategoryId: Long)

        /**
         * The method is called when the uploading to the catalog is finished.
         * @param uploadResult is result of the uploading.
         * @param description is detailed description of the uploading result.
         * @param originCategoryId is original identifier of the uploaded bookmarks category.
         * @param resultCategoryId is identifier of the uploaded category after finishing.
         * In the case of bookmarks modification during uploading
         */
        fun onUploadFinished(
            uploadResult: UploadResult, description: String,
            originCategoryId: Long, resultCategoryId: Long
        )
    }

    open class DefaultBookmarksCatalogListener : BookmarksCatalogListener {
        override fun onImportStarted(serverId: String) { /* do noting by default */
        }

        override fun onImportFinished(
            serverId: String,
            catId: Long,
            successful: Boolean
        ) { /* do noting by default */
        }

        override fun onTagsReceived(
            successful: Boolean, tagsGroups: List<CatalogTagsGroup>,
            tagsLimit: Int
        ) { /* do noting by default */
        }

        override fun onCustomPropertiesReceived(
            successful: Boolean,
            properties: List<CatalogCustomProperty>
        ) { /* do noting by default */
        }

        override fun onUploadStarted(originCategoryId: Long) { /* do noting by default */
        }

        override fun onUploadFinished(
            uploadResult: UploadResult, description: String,
            originCategoryId: Long, resultCategoryId: Long
        ) { /* do noting by default */
        }
    }

    enum class UploadResult {
        UPLOAD_RESULT_SUCCESS, UPLOAD_RESULT_NETWORK_ERROR, UPLOAD_RESULT_SERVER_ERROR, UPLOAD_RESULT_AUTH_ERROR,  /* Broken file */
        UPLOAD_RESULT_MALFORMED_DATA_ERROR,  /* Edit on web */
        UPLOAD_RESULT_ACCESS_ERROR, UPLOAD_RESULT_INVALID_CALL
    }

    class BookmarkCategoriesCache :
        Observable<DataChangedListener<*>?>() {
        private val mCategories: MutableList<BookmarkCategory> =
            ArrayList()

        fun update(categories: List<BookmarkCategory>) {
            mCategories.clear()
            mCategories.addAll(categories)
            notifyChanged()
        }

        val categories: List<BookmarkCategory>
            get() = mCategories
    }
}