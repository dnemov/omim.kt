package com.mapswithme.maps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.mapswithme.maps.MwmApplication.Companion.from
import com.mapswithme.maps.SplashActivity
import com.mapswithme.maps.ads.Banner
import com.mapswithme.maps.analytics.AdvertisingObserver
import com.mapswithme.maps.base.BaseActivity
import com.mapswithme.maps.base.BaseActivityDelegate
import com.mapswithme.maps.base.Detachable
import com.mapswithme.maps.downloader.UpdaterDialogFragment
import com.mapswithme.maps.editor.ViralFragment
import com.mapswithme.maps.editor.ViralFragment.Companion.shouldDisplay
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.news.OnboardingStep
import com.mapswithme.maps.onboarding.BaseNewsFragment.NewsDialogListener
import com.mapswithme.maps.onboarding.NewsFragment.Companion.showOn
import com.mapswithme.maps.onboarding.WelcomeDialogFragment
import com.mapswithme.maps.onboarding.WelcomeDialogFragment.Companion.isAgreementDeclined
import com.mapswithme.maps.onboarding.WelcomeDialogFragment.Companion.isFirstLaunch
import com.mapswithme.maps.onboarding.WelcomeDialogFragment.Companion.showOnboardinSteps
import com.mapswithme.maps.onboarding.WelcomeDialogFragment.Companion.showOnboardinStepsStartWith
import com.mapswithme.maps.onboarding.WelcomeDialogFragment.OnboardingStepPassedListener
import com.mapswithme.maps.onboarding.WelcomeDialogFragment.PolicyAgreementListener
import com.mapswithme.maps.permissions.PermissionsDialogFragment
import com.mapswithme.maps.permissions.PermissionsDialogFragment.Companion.show
import com.mapswithme.maps.permissions.StoragePermissionsDialogFragment
import com.mapswithme.maps.permissions.StoragePermissionsDialogFragment.Companion.find
import com.mapswithme.util.*
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory
import com.my.tracker.MyTracker

class SplashActivity : AppCompatActivity(), NewsDialogListener, BaseActivity,
    PolicyAgreementListener, OnboardingStepPassedListener {
    private var mIvLogo: View? = null
    private var mAppName: View? = null
    private var mPermissionsGranted = false
    private var mNeedStoragePermission = false
    private var mCanceled = false
    var isWaitForAdvertisingInfo = false
        private set
    private val mUserAgreementDelayedTask =
        Runnable { WelcomeDialogFragment.show(this@SplashActivity) }
    private val mOnboardingStepsTask = Runnable {
        if (mCurrentOnboardingStep != null) {
            showOnboardinStepsStartWith(
                this@SplashActivity,
                mCurrentOnboardingStep!!
            )
            return@Runnable
        }
        showOnboardinSteps(this@SplashActivity)
    }
    private val mPermissionsDelayedTask = Runnable {
        show(
            this@SplashActivity,
            REQUEST_PERMISSIONS
        )
    }
    private val mInitCoreDelayedTask: Runnable = object : Runnable {
        override fun run() {
            val app = application as MwmApplication
            if (app.arePlatformAndCoreInitialized()) {
                UiThread.runLater(mFinalDelayedTask)
                return
            }
            val mediator =
                from(applicationContext).mediator
            if (!mediator.isAdvertisingInfoObtained) {
                LOGGER.i(
                    TAG,
                    "Advertising info not obtained yet, wait..."
                )
                isWaitForAdvertisingInfo = true
                return
            }
            isWaitForAdvertisingInfo = false
            if (!mediator.isLimitAdTrackingEnabled) {
                LOGGER.i(
                    TAG,
                    "Limit ad tracking disabled, sensitive tracking initialized"
                )
                mediator.initSensitiveData()
                MyTracker.trackLaunchManually(this@SplashActivity)
            } else {
                LOGGER.i(
                    TAG,
                    "Limit ad tracking enabled, sensitive tracking not initialized"
                )
            }
            init()
            LOGGER.i(
                TAG,
                "Core initialized: " + app.arePlatformAndCoreInitialized()
            )
            if (app.arePlatformAndCoreInitialized() && mediator.isLimitAdTrackingEnabled) {
                LOGGER.i(
                    TAG,
                    "Limit ad tracking enabled, rb banners disabled."
                )
                mediator.disableAdProvider(Banner.Type.TYPE_RB)
            }
            //    Run delayed task because resumeDialogs() must see the actual value of mCanceled flag,
//    since onPause() callback can be blocked because of UI thread is busy with framework
//    initialization.
            UiThread.runLater(mFinalDelayedTask)
        }
    }
    private val mFinalDelayedTask = Runnable { resumeDialogs() }
    private val mBaseDelegate = BaseActivityDelegate(this)
    private val mAdvertisingObserver = AdvertisingInfoObserver()
    private var mCurrentOnboardingStep: OnboardingStep? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBaseDelegate.onCreate()
        handleOnboardingStep(savedInstanceState)
        handleUpdateMapsFragmentCorrectly(savedInstanceState)
        UiThread.cancelDelayedTasks(mUserAgreementDelayedTask)
        UiThread.cancelDelayedTasks(mOnboardingStepsTask)
        UiThread.cancelDelayedTasks(mPermissionsDelayedTask)
        UiThread.cancelDelayedTasks(mInitCoreDelayedTask)
        UiThread.cancelDelayedTasks(mFinalDelayedTask)
        Counters.initCounters(this)
        initView()
    }

    private fun handleOnboardingStep(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        if (!savedInstanceState.containsKey(EXTRA_CURRENT_ONBOARDING_STEP)) return
        val step =
            savedInstanceState.getInt(EXTRA_CURRENT_ONBOARDING_STEP)
        mCurrentOnboardingStep = OnboardingStep.values()[step]
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mBaseDelegate.onNewIntent(intent)
    }

    private fun handleUpdateMapsFragmentCorrectly(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        val fm = supportFragmentManager
        val updaterFragment = fm
            .findFragmentByTag(UpdaterDialogFragment::class.java.name) as DialogFragment?
            ?: return
        // If the user revoked the external storage permission while the app was killed
// we can't update maps automatically during recovering process, so just dismiss updater fragment
// and ask the user to grant the permission.
        if (!PermissionsUtils.isExternalStorageGranted()) {
            fm.beginTransaction().remove(updaterFragment).commitAllowingStateLoss()
            fm.executePendingTransactions()
            StoragePermissionsDialogFragment.show(this)
        } else { // If external permissions are still granted we just need to check platform
// and core initialization, because we may be in the recovering process,
// i.e. method onResume() may not be invoked in that case.
            if (!MwmApplication.get().arePlatformAndCoreInitialized()) {
                init()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mBaseDelegate.onStart()
        mAdvertisingObserver.attach(this)
        val mediator = from(this).mediator
        LOGGER.d(TAG, "Add advertising observer")
        mediator.addAdvertisingObserver(mAdvertisingObserver)
    }

    override fun onResume() {
        super.onResume()
        mBaseDelegate.onResume()
        mCanceled = false
        if (Counters.isMigrationNeeded()) {
            Config.migrateCountersToSharedPrefs()
            Counters.setMigrationExecuted()
        }
        val isFirstLaunch = isFirstLaunch(this)
        if (isFirstLaunch) from(this).isFirstLaunch = true
        var isWelcomeFragmentOnScreen = false
        val welcomeFragment =
            WelcomeDialogFragment.find(this)
        if (welcomeFragment != null) {
            isWelcomeFragmentOnScreen = true
            welcomeFragment.dismissAllowingStateLoss()
        }
        if (isFirstLaunch || isWelcomeFragmentOnScreen) {
            if (isAgreementDeclined(this)) {
                UiThread.runLater(
                    mUserAgreementDelayedTask,
                    FIRST_START_DELAY
                )
                return
            } else {
                if (processPermissionGranting()) {
                    UiThread.runLater(
                        mOnboardingStepsTask,
                        DELAY
                    )
                    return
                }
            }
        }
        if (processPermissionGranting()) runInitCoreTask()
    }

    private fun processPermissionGranting(): Boolean {
        mPermissionsGranted = PermissionsUtils.isExternalStorageGranted()
        val storagePermissionsDialog =
            find(this)
        var permissionsDialog =
            PermissionsDialogFragment.find(this)
        if (!mPermissionsGranted) {
            if (mNeedStoragePermission || storagePermissionsDialog != null) {
                permissionsDialog?.dismiss()
                if (storagePermissionsDialog == null) StoragePermissionsDialogFragment.show(this)
                return false
            }
            permissionsDialog = PermissionsDialogFragment.find(this)
            if (permissionsDialog == null) UiThread.runLater(
                mPermissionsDelayedTask,
                DELAY
            )
            return false
        }
        permissionsDialog?.dismiss()
        storagePermissionsDialog?.dismiss()
        return true
    }

    private fun runInitCoreTask() {
        UiThread.runLater(
            mInitCoreDelayedTask,
            DELAY
        )
    }

    override fun onPause() {
        super.onPause()
        mBaseDelegate.onPause()
        mCanceled = true
        UiThread.cancelDelayedTasks(mUserAgreementDelayedTask)
        UiThread.cancelDelayedTasks(mOnboardingStepsTask)
        UiThread.cancelDelayedTasks(mPermissionsDelayedTask)
        UiThread.cancelDelayedTasks(mInitCoreDelayedTask)
        UiThread.cancelDelayedTasks(mFinalDelayedTask)
    }

    override fun onStop() {
        super.onStop()
        mBaseDelegate.onStop()
        mAdvertisingObserver.detach()
        val mediator = from(this).mediator
        LOGGER.d(
            TAG,
            "Remove advertising observer"
        )
        mediator.removeAdvertisingObserver(mAdvertisingObserver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mCurrentOnboardingStep != null) outState.putInt(
            EXTRA_CURRENT_ONBOARDING_STEP,
            mCurrentOnboardingStep!!.ordinal
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mBaseDelegate.onDestroy()
    }

    private fun resumeDialogs() {
        if (mCanceled) return
        val app = application as MwmApplication
        if (!app.arePlatformAndCoreInitialized()) {
            showExternalStorageErrorDialog()
            return
        }
        val showNews = showOn(this, this)
        if (!showNews) {
            if (shouldDisplay()) {
                UiUtils.hide(mIvLogo!!, mAppName!!)
                val dialog = ViralFragment()
                dialog.onDismissListener(Runnable { onDialogDone() })
                dialog.show(supportFragmentManager, "")
            } else {
                processNavigation()
            }
        } else {
            UiUtils.hide(mIvLogo!!, mAppName!!)
        }
    }

    private fun showExternalStorageErrorDialog() {
        val dialog =
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_error_storage_title)
                .setMessage(R.string.dialog_error_storage_message)
                .setPositiveButton(R.string.ok) { dialog, which -> finish() }
                .setCancelable(false)
                .create()
        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size == 0) return
        mPermissionsGranted = PermissionsUtils.computePermissionsResult(permissions, grantResults)
            .isExternalStorageGranted
        mNeedStoragePermission = !mPermissionsGranted
    }

    override fun onDialogDone() {
        processNavigation()
    }

    override fun onPolicyAgreementApplied() {
        val permissionsGranted = processPermissionGranting()
        if (!permissionsGranted) return
        val isFirstLaunch = isFirstLaunch(this)
        if (isFirstLaunch) {
            UiThread.runLater(
                mOnboardingStepsTask,
                DELAY
            )
            return
        }
        runInitCoreTask()
    }

    override fun onLastOnboardingStepPassed() {
        runInitCoreTask()
    }

    override fun onOnboardingStepCancelled() {
        finish()
    }

    override fun onOnboardingStepPassed(step: OnboardingStep) {
        mCurrentOnboardingStep = step
    }

    private fun initView() {
        UiUtils.setupStatusBar(this)
        setContentView(R.layout.activity_splash)
        mIvLogo = findViewById(R.id.iv__logo)
        mAppName = findViewById(R.id.tv__app_name)
    }

    private fun init() {
        val app = from(this)
        val success = app.initCore()
        if (!success || !app.isFirstLaunch) return
        LocationHelper.INSTANCE.onEnteredIntoFirstRun()
        if (!LocationHelper.INSTANCE.isActive) LocationHelper.INSTANCE.start()
    }

    private fun processNavigation() {
        val input = intent
        var result = Intent(this, DownloadResourcesLegacyActivity::class.java)
        if (input != null) {
            if (input.hasExtra(EXTRA_ACTIVITY_TO_START)) {
                result = Intent(
                    this,
                    input.getSerializableExtra(EXTRA_ACTIVITY_TO_START) as Class<out Activity?>
                )
            }
            val initialIntent =
                if (input.hasExtra(EXTRA_INITIAL_INTENT)) input.getParcelableExtra(
                    EXTRA_INITIAL_INTENT
                ) else input
            result.putExtra(EXTRA_INITIAL_INTENT, initialIntent)
        }
        startActivity(result)
        finish()
    }

    override fun get(): Activity {
        return this
    }

    override fun getThemeResourceId(theme: String): Int {
        if (ThemeUtils.isDefaultTheme(theme)) return R.style.MwmTheme
        if (ThemeUtils.isNightTheme(theme)) return R.style.MwmTheme_Night
        throw IllegalArgumentException("Attempt to apply unsupported theme: $theme")
    }

    private class AdvertisingInfoObserver : AdvertisingObserver,
        Detachable<SplashActivity?> {
        private var mActivity: SplashActivity? = null
        override fun onAdvertisingInfoObtained() {
            LOGGER.i(
                TAG,
                "Advertising info obtained"
            )
            if (mActivity == null) return
            if (!mActivity!!.isWaitForAdvertisingInfo) {
                LOGGER.i(
                    TAG,
                    "Advertising info not waited"
                )
                return
            }
            mActivity!!.runInitCoreTask()
        }

        override fun attach(`object`: SplashActivity?) {
            mActivity = `object`
        }

        override fun detach() {
            mActivity = null
        }
    }

    companion object {
        private const val EXTRA_CURRENT_ONBOARDING_STEP = "extra_current_onboarding_step"
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = SplashActivity::class.java.simpleName
        private const val EXTRA_ACTIVITY_TO_START = "extra_activity_to_start"
        const val EXTRA_INITIAL_INTENT = "extra_initial_intent"
        private const val REQUEST_PERMISSIONS = 1
        private const val FIRST_START_DELAY: Long = 300
        private const val DELAY: Long = 100
        fun start(
            context: Context,
            activityToStart: Class<out Activity>?,
            initialIntent: Intent?
        ) {
            val intent = Intent(context, SplashActivity::class.java)
            if (activityToStart != null) intent.putExtra(
                EXTRA_ACTIVITY_TO_START,
                activityToStart
            )
            if (initialIntent != null) intent.putExtra(
                EXTRA_INITIAL_INTENT,
                initialIntent
            )
            context.startActivity(intent)
        }
    }
}