package com.mapswithme.maps.base

import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.SplashActivity
import com.mapswithme.util.*
import com.mapswithme.util.log.LoggerFactory

abstract class BaseMwmFragmentActivity : AppCompatActivity(), BaseActivity {
    private val mBaseDelegate = BaseActivityDelegate(this)
    private var mSafeCreated = false
    override fun get(): Activity {
        return this
    }

    @StyleRes
    override fun getThemeResourceId(theme: String): Int {
        if (ThemeUtils.isDefaultTheme(theme)) return R.style.MwmTheme
        if (ThemeUtils.isNightTheme(theme)) return R.style.MwmTheme_Night
        throw IllegalArgumentException("Attempt to apply unsupported theme: $theme")
    }

    /**
     * Shows splash screen and initializes the core in case when it was not initialized.
     *
     * Do not override this method!
     * Use [.onSafeCreate]
     */
    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        mBaseDelegate.onCreate()
        // An intent that was skipped due to core wasn't initialized has to be used
// as a target intent for this activity, otherwise all input extras will be lost
// in a splash activity loop.
        val initialIntent =
            intent.getParcelableExtra<Intent>(SplashActivity.EXTRA_INITIAL_INTENT)
        if (initialIntent != null) intent = initialIntent
        if (!MwmApplication.get().arePlatformAndCoreInitialized()
            || !PermissionsUtils.isExternalStorageGranted()
        ) {
            super.onCreate(savedInstanceState)
            goToSplashScreen(intent)
            return
        }
        super.onCreate(savedInstanceState)
        onSafeCreate(savedInstanceState)
    }

    /**
     * Use this safe method instead of [.onCreate].
     * When this method is called, the core is already initialized.
     */
    @CallSuper
    protected open fun onSafeCreate(savedInstanceState: Bundle?) {
        volumeControlStream = AudioManager.STREAM_MUSIC
        val layoutId = contentLayoutResId
        if (layoutId != 0) setContentView(layoutId)
        if (useTransparentStatusBar()) UiUtils.setupStatusBar(this)
        if (useColorStatusBar()) UiUtils.setupColorStatusBar(this, statusBarColor)
        // Use full-screen on Kindle Fire only
        if (Utils.isAmazonDevice) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        }
        attachDefaultFragment()
        mBaseDelegate.onSafeCreate()
        mSafeCreated = true
    }


    protected val statusBarColor: Int
        @ColorRes get() {
            val theme = Config.getCurrentUiTheme()
            if (ThemeUtils.isDefaultTheme(theme)) return R.color.bg_statusbar
            if (ThemeUtils.isNightTheme(theme)) return R.color.bg_statusbar_night
            throw IllegalArgumentException("Attempt to apply unsupported theme: $theme")
        }

    protected open fun useColorStatusBar(): Boolean {
        return false
    }

    protected open fun useTransparentStatusBar(): Boolean {
        return true
    }

    @CallSuper
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mBaseDelegate.onNewIntent(intent)
    }

    @CallSuper
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mBaseDelegate.onPostCreate()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        mBaseDelegate.onDestroy()
        if (!mSafeCreated) return
        onSafeDestroy()
    }

    /**
     * Use this safe method instead of [.onDestroy].
     * When this method is called, the core is already initialized and
     * [.onSafeCreate] was called.
     */
    @CallSuper
    protected open fun onSafeDestroy() {
        mBaseDelegate.onSafeDestroy()
        mSafeCreated = false
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        mBaseDelegate.onStart()
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        mBaseDelegate.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onHomeOptionItemSelected()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected open fun onHomeOptionItemSelected() {
        onBackPressed()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        if (!PermissionsUtils.isExternalStorageGranted()) {
            goToSplashScreen(null)
            return
        }
        mBaseDelegate.onResume()
    }

    @CallSuper
    override fun onPostResume() {
        super.onPostResume()
        mBaseDelegate.onPostResume()
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        mBaseDelegate.onPause()
    }

    protected val toolbar: Toolbar?
        get() = findViewById<View>(R.id.toolbar) as Toolbar

    protected fun displayToolbarAsActionBar() {
        setSupportActionBar(toolbar)
    }

    override fun onBackPressed() {
        if (fragmentClass == null) {
            super.onBackPressed()
            return
        }
        val manager = supportFragmentManager
        val name = fragmentClass!!.name
        val fragment = manager.findFragmentByTag(name)
        if (fragment == null) {
            super.onBackPressed()
            return
        }
        if (onBackPressedInternal(fragment)) return
        super.onBackPressed()
    }

    private fun onBackPressedInternal(currentFragment: Fragment): Boolean {
        return try {
            val listener = currentFragment as OnBackPressListener
            listener.onBackPressed()
        } catch (e: ClassCastException) {
            val logger =
                LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
            val tag = this.javaClass.simpleName
            logger.i(tag, "Fragment '$currentFragment' doesn't handle back press by itself.")
            false
        }
    }

    /**
     * Override to set custom content view.
     * @return layout resId.
     */
    protected open val contentLayoutResId: Int
        get() = 0

    protected fun attachDefaultFragment() {
        val clazz = fragmentClass
        if (clazz != null) replaceFragment(clazz, intent.extras, null)
    }

    /**
     * Replace attached fragment with the new one.
     */
    open fun replaceFragment(
        fragmentClass: Class<out Fragment?>,
        args: Bundle?,
        completionListener: Runnable?
    ) {
        val resId = fragmentContentResId
        check(!(resId <= 0 || findViewById<View?>(resId) == null)) { "Fragment can't be added, since getFragmentContentResId() isn't implemented or returns wrong resourceId." }
        val name = fragmentClass.name
        val potentialInstance =
            supportFragmentManager.findFragmentByTag(name)
        if (potentialInstance == null) {
            val fragment =
                Fragment.instantiate(this, name, args)
            supportFragmentManager.beginTransaction()
                .replace(resId, fragment, name)
                .commitAllowingStateLoss()
            supportFragmentManager.executePendingTransactions()
            completionListener?.run()
        }
    }

    /**
     * Override to automatically attach fragment in onCreate. Tag applied to fragment in back stack is set to fragment name, too.
     * WARNING : if custom layout for activity is set, getFragmentContentResId() must be implemented, too.
     * @return class of the fragment, eg FragmentClass.getClass()
     */
    protected open val fragmentClass: Class<out Fragment>?
        get() = null

    /**
     * Get resource id for the fragment. That must be implemented to return correct resource id, if custom layout is set.
     * @return resourceId for the fragment
     */
    protected open val fragmentContentResId: Int
        protected get() = android.R.id.content

    private fun goToSplashScreen(initialIntent: Intent?) {
        SplashActivity.start(this, javaClass, initialIntent)
        finish()
    }
}