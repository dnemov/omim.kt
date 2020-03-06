package com.mapswithme.maps.widget.menu

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import com.mapswithme.maps.ClickMenuDelegate
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.MwmActivity.*
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.downloader.MapManager.nativeGetUpdateInfo
import com.mapswithme.maps.routing.RoutingController.Companion.get
import com.mapswithme.util.Animations
import com.mapswithme.util.Graphics
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.StatisticValueConverter
import java.util.*

class MainMenu(
    frame: View,
    itemClickListener: ItemClickListener<BaseMenu.Item>
) : BaseMenu(frame, itemClickListener) {
    enum class State {
        MENU {
            override fun showToggle(): Boolean {
                return false
            }
        },
        NAVIGATION, ROUTE_PREPARE {
            override fun showToggle(): Boolean {
                return false
            }
        };

        open fun showToggle(): Boolean {
            return true
        }
    }

    private val mButtonsWidth = UiUtils.dimen(R.dimen.menu_line_button_width)
    private val mPanelWidth = UiUtils.dimen(R.dimen.panel_width)
    private val mButtonsFrame: View
    private val mRoutePlanFrame: View?
    private lateinit var mAnimationSpacer: View
    private var mAnimationSymmetricalGap: View? = null
    private val mNewsMarker: View
    private val mNewsCounter: TextView
    private var mCollapsed = false
    private val mCollapseViews: MutableList<View> =
        ArrayList()
    private lateinit var mToggle: MenuToggle
    // Maps Item into button view placed on mContentFrame
    private val mItemViews: MutableMap<Item, View?> =
        HashMap()
    val leftAnimationTrackListener: LeftAnimationTrackListener =
        object : LeftAnimationTrackListener {
            private var mSymmetricalGapScale = 0f
            override fun onTrackStarted(collapsed: Boolean) {
                for (v in mCollapseViews) {
                    if (collapsed) UiUtils.show(v)
                    v.alpha = if (collapsed) 0.0f else 1.0f
                    v.animate()
                        .alpha(if (collapsed) 1.0f else 0.0f)
                        .start()
                }
                mToggle.setCollapsed(!collapsed, true)
                mSymmetricalGapScale = mButtonsWidth.toFloat() / mPanelWidth
            }

            override fun onTrackFinished(collapsed: Boolean) {
                mCollapsed = collapsed
                updateMarker()
                if (collapsed) for (v in mCollapseViews) UiUtils.hide(v)
            }

            override fun onTrackLeftAnimation(offset: Float) {
                var lp =
                    mAnimationSpacer.layoutParams as MarginLayoutParams
                lp.rightMargin = offset.toInt()
                mAnimationSpacer.layoutParams = lp
                if (mAnimationSymmetricalGap == null) return
                lp = mAnimationSymmetricalGap!!.layoutParams as MarginLayoutParams
                lp.width = mButtonsWidth - (mSymmetricalGapScale * offset) as Int
                mAnimationSymmetricalGap!!.layoutParams = lp
            }
        }

    enum class Item(override val viewId: Int) :
        BaseMenu.Item, StatisticValueConverter<String?> {
        MENU(R.id.toggle) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return MenuClickDelegate(activity, item)
            }
        },
        ADD_PLACE(R.id.add_place) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return AddPlaceDelegate(activity, item)
            }
        },
        DOWNLOAD_GUIDES(R.id.download_guides) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return DownloadGuidesDelegate(activity, item)
            }
        },
        HOTEL_SEARCH(R.id.hotel_search) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return HotelSearchDelegate(activity, item)
            }

            override fun toStatisticValue(): String {
                return "booking.com"
            }
        },
        SEARCH(R.id.search) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return SearchClickDelegate(activity, item)
            }
        },
        POINT_TO_POINT(R.id.p2p) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return PointToPointDelegate(activity, item)
            }
        },
        DISCOVERY(R.id.discovery) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return DiscoveryDelegate(activity, item)
            }
        },
        BOOKMARKS(R.id.bookmarks) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return BookmarksDelegate(activity, item)
            }
        },
        SHARE_MY_LOCATION(R.id.share) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return ShareMyLocationDelegate(activity, item)
            }
        },
        DOWNLOAD_MAPS(R.id.download_maps) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return DownloadMapsDelegate(activity, item)
            }
        },
        SETTINGS(R.id.settings) {
            override fun createClickDelegate(
                activity: MwmActivity,
                item: Item
            ): ClickMenuDelegate {
                return SettingsDelegate(activity, item)
            }
        };

        fun onClicked(
            activity: MwmActivity,
            item: Item
        ) {
            val delegate = createClickDelegate(activity, item)
            delegate.onMenuItemClick()
        }

        abstract fun createClickDelegate(
            activity: MwmActivity,
            item: Item
        ): ClickMenuDelegate

        override fun toStatisticValue(): String {
            return name.toLowerCase(Locale.ENGLISH)
        }

    }

    public override fun mapItem(
        item: BaseMenu.Item,
        frame: View
    ): View? {
        val res = super.mapItem(item, frame)
        if (res != null && TAG_COLLAPSE == res.tag) mCollapseViews.add(res)
        return res
    }

    private fun mapItem(item: Item) {
        mapItem(item, mButtonsFrame)
        val view = mapItem(item, mContentFrame)
        mItemViews[item] = view
        if (view != null) Graphics.tint(view as TextView)
    }

    override fun adjustCollapsedItems() {
        for (v in mCollapseViews) {
            UiUtils.showIf(!mCollapsed, v)
            v.alpha = if (mCollapsed) 0.0f else 1.0f
        }
        if (mAnimationSymmetricalGap == null) return
        val lp = mAnimationSymmetricalGap?.layoutParams
        lp?.width = if (mCollapsed) 0 else mButtonsWidth
        mAnimationSymmetricalGap?.layoutParams = lp
    }

    public override fun afterLayoutMeasured(procAfterCorrection: Runnable?) {
        UiUtils.showIf(!get().isNavigating, super.frame)
        super.afterLayoutMeasured(procAfterCorrection)
    }

    override fun updateMarker() {
        val info = nativeGetUpdateInfo(null)
        val count = info?.filesCount ?: 0
        val show =
            count > 0 && !isOpen && (!mCollapsed || mCollapseViews.isEmpty())
        UiUtils.showIf(show, mNewsMarker)
        UiUtils.showIf(count > 0, mNewsCounter)
        if (count > 0) mNewsCounter.text = count.toString()
    }

    override fun setToggleState(
        open: Boolean,
        animate: Boolean
    ) {
        mToggle.setOpen(open, animate)
    }

    private fun init() {
        mapItem(Item.ADD_PLACE)
        mapItem(Item.DOWNLOAD_GUIDES)
        mapItem(Item.HOTEL_SEARCH)
        mapItem(Item.SEARCH)
        mapItem(Item.POINT_TO_POINT)
        mapItem(Item.DISCOVERY)
        mapItem(Item.BOOKMARKS)
        mapItem(Item.SHARE_MY_LOCATION)
        mapItem(Item.DOWNLOAD_MAPS)
        mapItem(Item.SETTINGS)
        adjustCollapsedItems()
        setState(State.MENU, false, false)
    }

    protected override val heightResId: Int
        protected get() = R.dimen.menu_line_height

    fun setState(
        state: State,
        animateToggle: Boolean,
        isFullScreen: Boolean
    ) {
        if (state !== State.NAVIGATION) {
            mToggle.show(state.showToggle())
            mToggle.setCollapsed(mCollapsed, animateToggle)
            val expandContent: Boolean
            val isRouting =
                state === State.ROUTE_PREPARE
            expandContent = if (mRoutePlanFrame == null) {
                UiUtils.show(mButtonsFrame)
                false
            } else {
                UiUtils.showIf(
                    state === State.MENU,
                    mButtonsFrame
                )
                UiUtils.showIf(isRouting, mRoutePlanFrame)
                if (isRouting) mToggle.hide()
                isRouting
            }
            UiUtils.showIf(
                expandContent,
                mItemViews[Item.SEARCH],
                mItemViews[Item.BOOKMARKS]
            )
            setVisible(Item.ADD_PLACE, !isRouting)
        }
        if (mLayoutMeasured) {
            show(state !== State.NAVIGATION && !isFullScreen)
            UiUtils.showIf(
                state === State.MENU,
                mButtonsFrame
            )
            UiUtils.showIf(
                state === State.ROUTE_PREPARE,
                mRoutePlanFrame
            )
            mContentFrame.measure(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            mContentHeight = mContentFrame.measuredHeight
        }
    }

    fun setEnabled(
        item: Item,
        enable: Boolean
    ) {
        val button = mButtonsFrame.findViewById<View>(item.viewId) ?: return
        button.alpha = if (enable) 1.0f else 0.4f
        button.isEnabled = enable
    }

    private fun setVisible(
        item: Item,
        show: Boolean
    ) {
        val itemInButtonsFrame =
            mButtonsFrame.findViewById<View>(item.viewId)
        if (itemInButtonsFrame != null) UiUtils.showIf(show, itemInButtonsFrame)
        if (mItemViews[item] != null) UiUtils.showIf(show, mItemViews[item])
    }

    fun showLineFrame(show: Boolean) {
        if (show) {
            UiUtils.hide(super.frame)
            Animations.appearSliding(super.frame, Animations.BOTTOM, null)
        } else {
            UiUtils.show(super.frame)
            Animations.disappearSliding(super.frame, Animations.BOTTOM, null)
        }
    }

    companion object {
        private val TAG_COLLAPSE =
            MwmApplication.get().getString(R.string.tag_menu_collapse)
    }

    init {
        mButtonsFrame = mLineFrame.findViewById(R.id.buttons_frame)
        mRoutePlanFrame = mLineFrame.findViewById(R.id.routing_plan_frame)
        mAnimationSpacer = super.frame.findViewById(R.id.animation_spacer)
        mAnimationSymmetricalGap =
            mButtonsFrame.findViewById(R.id.symmetrical_gap)
        mToggle = MenuToggle(mLineFrame, heightResId)
        mapItem(Item.MENU, mLineFrame)
        mNewsMarker = mButtonsFrame.findViewById(R.id.marker)
        mNewsCounter = mContentFrame.findViewById<View>(R.id.counter) as TextView
        init()
    }
}