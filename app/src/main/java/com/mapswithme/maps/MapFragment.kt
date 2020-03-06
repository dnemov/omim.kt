package com.mapswithme.maps

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import androidx.appcompat.app.AlertDialog
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.util.Config
import com.mapswithme.util.UiUtils
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory

class MapFragment : BaseMwmFragment(), OnTouchListener, SurfaceHolder.Callback {
    private var mHeight = 0
    private var mWidth = 0
    private var mRequireResize = false
    var isContextCreated = false
        private set
    private var mSurfaceAttached = false
    private var mLaunchByDeepLink = false
    private var mUiThemeOnPause: String? = null
    private lateinit var mSurfaceView: SurfaceView
    private var mMapRenderingListener: MapRenderingListener? = null
    private fun setupWidgets(width: Int, height: Int) {
        mHeight = height
        mWidth = width
        nativeCleanWidgets()
        if (!sWasCopyrightDisplayed) {
            nativeSetupWidget(
                WIDGET_COPYRIGHT,
                UiUtils.dimen(R.dimen.margin_ruler_left).toFloat(),
                mHeight - UiUtils.dimen(R.dimen.margin_ruler_bottom).toFloat(),
                ANCHOR_LEFT_BOTTOM
            )
            sWasCopyrightDisplayed = true
        }
        setupRuler(0, false)
        setupWatermark(0, false)
        nativeSetupWidget(
            WIDGET_SCALE_FPS_LABEL,
            UiUtils.dimen(R.dimen.margin_base).toFloat(),
            UiUtils.dimen(R.dimen.margin_base).toFloat(),
            ANCHOR_LEFT_TOP
        )
        setupCompass(UiUtils.getCompassYOffset(requireContext()), false)
    }

    fun setupCompass(offsetY: Int, forceRedraw: Boolean) {
        val navPadding = UiUtils.dimen(R.dimen.nav_frame_padding)
        val marginX = UiUtils.dimen(R.dimen.margin_compass) + navPadding
        val marginY = UiUtils.dimen(R.dimen.margin_compass_top) + navPadding
        nativeSetupWidget(
            WIDGET_COMPASS,
            mWidth - marginX.toFloat(),
            offsetY + marginY.toFloat(),
            ANCHOR_CENTER
        )
        if (forceRedraw && isContextCreated) nativeApplyWidgets()
    }

    fun setupRuler(offsetY: Int, forceRedraw: Boolean) {
        nativeSetupWidget(
            WIDGET_RULER,
            UiUtils.dimen(R.dimen.margin_ruler_left).toFloat(),
            mHeight - UiUtils.dimen(R.dimen.margin_ruler_bottom) + offsetY.toFloat(),
            ANCHOR_LEFT_BOTTOM
        )
        if (forceRedraw && isContextCreated) nativeApplyWidgets()
    }

    fun setupWatermark(offsetY: Int, forceRedraw: Boolean) {
        nativeSetupWidget(
            WIDGET_WATERMARK,
            mWidth - UiUtils.dimen(R.dimen.margin_watermark_right).toFloat(),
            mHeight - UiUtils.dimen(R.dimen.margin_watermark_bottom) + offsetY.toFloat(),
            ANCHOR_RIGHT_BOTTOM
        )
        if (forceRedraw && isContextCreated) nativeApplyWidgets()
    }

    private fun reportUnsupported() {
        AlertDialog.Builder(requireActivity())
            .setMessage(getString(R.string.unsupported_phone))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.close)) { dlg, which ->
                requireActivity().moveTaskToBack(
                    true
                )
            }
            .show()
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        if (isThemeChangingProcess) {
            LOGGER.d(
                TAG,
                "Activity is being recreated due theme changing, skip 'surfaceCreated' callback"
            )
            return
        }
        LOGGER.d(
            TAG,
            "surfaceCreated, mSurfaceCreated = " + isContextCreated
        )
        val surface = surfaceHolder.surface
        if (nativeIsEngineCreated()) {
            if (!nativeAttachSurface(surface)) {
                reportUnsupported()
                return
            }
            isContextCreated = true
            mSurfaceAttached = true
            mRequireResize = true
            nativeResumeSurfaceRendering()
            return
        }
        mRequireResize = false
        val rect = surfaceHolder.surfaceFrame
        setupWidgets(rect.width(), rect.height())
        val metrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(metrics)
        val exactDensityDpi = metrics.densityDpi.toFloat()
        val firstStart = MwmApplication.from(requireActivity()).isFirstLaunch
        if (!nativeCreateEngine(
                surface, exactDensityDpi.toInt(), firstStart, mLaunchByDeepLink,
                BuildConfig.VERSION_CODE
            )
        ) {
            reportUnsupported()
            return
        }
        if (firstStart) {
            UiThread.runLater(Runnable { LocationHelper.INSTANCE.onExitFromFirstRun() })
        }
        isContextCreated = true
        mSurfaceAttached = true
        nativeResumeSurfaceRendering()
        if (mMapRenderingListener != null) mMapRenderingListener!!.onRenderingCreated()
    }

    override fun surfaceChanged(
        surfaceHolder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        if (isThemeChangingProcess) {
            LOGGER.d(
                TAG,
                "Activity is being recreated due theme changing, skip 'surfaceChanged' callback"
            )
            return
        }
        LOGGER.d(
            TAG,
            "surfaceChanged, mSurfaceCreated = " + isContextCreated
        )
        if (!isContextCreated || !mRequireResize && surfaceHolder.isCreating) return
        val surface = surfaceHolder.surface
        nativeSurfaceChanged(surface, width, height)
        mRequireResize = false
        setupWidgets(width, height)
        nativeApplyWidgets()
        if (mMapRenderingListener != null) mMapRenderingListener!!.onRenderingRestored()
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        LOGGER.d(TAG, "surfaceDestroyed")
        destroySurface()
    }

    fun destroySurface() {
        LOGGER.d(
            TAG, "destroySurface, mSurfaceCreated = " + isContextCreated +
                    ", mSurfaceAttached = " + mSurfaceAttached + ", isAdded = " + isAdded
        )
        if (!isContextCreated || !mSurfaceAttached || !isAdded) return
        nativeDetachSurface(!requireActivity().isChangingConfigurations)
        isContextCreated = !nativeDestroySurfaceOnDetach()
        mSurfaceAttached = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mMapRenderingListener = context as MapRenderingListener
    }

    override fun onDetach() {
        super.onDetach()
        mMapRenderingListener = null
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        retainInstance = true
        val args = arguments
        if (args != null) mLaunchByDeepLink =
            args.getBoolean(ARG_LAUNCH_BY_DEEP_LINK)
    }

    override fun onStart() {
        super.onStart()
        nativeSetRenderingInitializationFinishedListener(mMapRenderingListener)
        LOGGER.d(TAG, "onStart")
    }

    override fun onStop() {
        super.onStop()
        nativeSetRenderingInitializationFinishedListener(null)
        LOGGER.d(TAG, "onStop")
    }

    private val isThemeChangingProcess: Boolean
        private get() = mUiThemeOnPause != null && mUiThemeOnPause != Config.getCurrentUiTheme()

    override fun onPause() {
        mUiThemeOnPause = Config.getCurrentUiTheme()
        // Pause/Resume can be called without surface creation/destroy.
        if (mSurfaceAttached) nativePauseSurfaceRendering()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Pause/Resume can be called without surface creation/destroy.
        if (mSurfaceAttached) nativeResumeSurfaceRendering()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mSurfaceView = view.findViewById(R.id.map_surfaceview)
        mSurfaceView.holder.addCallback(this)
        return view
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val count = event.pointerCount
        if (count == 0) return false
        var action = event.actionMasked
        var pointerIndex = event.actionIndex
        when (action) {
            MotionEvent.ACTION_POINTER_UP -> action = NATIVE_ACTION_UP
            MotionEvent.ACTION_UP -> {
                action = NATIVE_ACTION_UP
                pointerIndex = 0
            }
            MotionEvent.ACTION_POINTER_DOWN -> action = NATIVE_ACTION_DOWN
            MotionEvent.ACTION_DOWN -> {
                action = NATIVE_ACTION_DOWN
                pointerIndex = 0
            }
            MotionEvent.ACTION_MOVE -> {
                action = NATIVE_ACTION_MOVE
                pointerIndex = INVALID_POINTER_MASK
            }
            MotionEvent.ACTION_CANCEL -> action = NATIVE_ACTION_CANCEL
        }
        return when (count) {
            1 -> {
                nativeOnTouch(
                    action,
                    event.getPointerId(0),
                    event.x,
                    event.y,
                    INVALID_TOUCH_ID,
                    0f,
                    0f,
                    0
                )
                true
            }
            else -> {
                nativeOnTouch(
                    action,
                    event.getPointerId(0), event.getX(0), event.getY(0),
                    event.getPointerId(1), event.getX(1), event.getY(1), pointerIndex
                )
                true
            }
        }
    }

    companion object {
        const val ARG_LAUNCH_BY_DEEP_LINK = "launch_by_deep_link"
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = MapFragment::class.java.simpleName
        // Should correspond to android::MultiTouchAction from Framework.cpp
        private const val NATIVE_ACTION_UP = 0x01
        private const val NATIVE_ACTION_DOWN = 0x02
        private const val NATIVE_ACTION_MOVE = 0x03
        private const val NATIVE_ACTION_CANCEL = 0x04
        // Should correspond to gui::EWidget from skin.hpp
        private const val WIDGET_RULER = 0x01
        private const val WIDGET_COMPASS = 0x02
        private const val WIDGET_COPYRIGHT = 0x04
        private const val WIDGET_SCALE_FPS_LABEL = 0x08
        private const val WIDGET_WATERMARK = 0x10
        // Should correspond to dp::Anchor from drape_global.hpp
        private const val ANCHOR_CENTER = 0x00
        private const val ANCHOR_LEFT = 0x01
        private const val ANCHOR_RIGHT = ANCHOR_LEFT shl 1
        private const val ANCHOR_TOP = ANCHOR_RIGHT shl 1
        private const val ANCHOR_BOTTOM = ANCHOR_TOP shl 1
        private const val ANCHOR_LEFT_TOP =
            ANCHOR_LEFT or ANCHOR_TOP
        private const val ANCHOR_RIGHT_TOP =
            ANCHOR_RIGHT or ANCHOR_TOP
        private const val ANCHOR_LEFT_BOTTOM =
            ANCHOR_LEFT or ANCHOR_BOTTOM
        private const val ANCHOR_RIGHT_BOTTOM =
            ANCHOR_RIGHT or ANCHOR_BOTTOM
        // Should correspond to df::TouchEvent::INVALID_MASKED_POINTER from user_event_stream.cpp
        private const val INVALID_POINTER_MASK = 0xFF
        private const val INVALID_TOUCH_ID = -1
        private var sWasCopyrightDisplayed = false

        @JvmStatic external fun nativeCompassUpdated(
            magneticNorth: Double,
            trueNorth: Double,
            forceRedraw: Boolean
        )

        @JvmStatic external fun nativeScalePlus()
        @JvmStatic external fun nativeScaleMinus()
        @JvmStatic external fun nativeShowMapForUrl(url: String?): Boolean
        @JvmStatic external fun nativeIsEngineCreated(): Boolean
        @JvmStatic external fun nativeDestroySurfaceOnDetach(): Boolean
        @JvmStatic private external fun nativeCreateEngine(
            surface: Surface, density: Int,
            firstLaunch: Boolean,
            isLaunchByDeepLink: Boolean,
            appVersionCode: Int
        ): Boolean

        @JvmStatic private external fun nativeAttachSurface(surface: Surface): Boolean
        @JvmStatic private external fun nativeDetachSurface(destroySurface: Boolean)
        @JvmStatic private external fun nativePauseSurfaceRendering()
        @JvmStatic private external fun nativeResumeSurfaceRendering()
        @JvmStatic private external fun nativeSurfaceChanged(
            surface: Surface,
            w: Int,
            h: Int
        )

        @JvmStatic private external fun nativeOnTouch(
            actionType: Int,
            id1: Int,
            x1: Float,
            y1: Float,
            id2: Int,
            x2: Float,
            y2: Float,
            maskedPointer: Int
        )

        @JvmStatic private external fun nativeSetupWidget(
            widget: Int,
            x: Float,
            y: Float,
            anchor: Int
        )

        @JvmStatic private external fun nativeApplyWidgets()
        @JvmStatic private external fun nativeCleanWidgets()
        @JvmStatic private external fun nativeSetRenderingInitializationFinishedListener(
            listener: MapRenderingListener?
        )
    }
}