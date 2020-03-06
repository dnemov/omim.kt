package com.mapswithme.util

import androidx.fragment.app.FragmentManager
import com.mapswithme.maps.widget.StackedButtonDialogFragment
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import com.mapswithme.util.statistics.StatisticValueConverter
import com.mapswithme.util.statistics.Statistics
import java.util.concurrent.TimeUnit

class NetworkPolicy private constructor(private val mCanUseNetwork: Boolean) {
    enum class Type : StatisticValueConverter<String?> {
        ASK {
            override fun toStatisticValue(): String {
                return Statistics.ParamValue.ASK
            }
        },
        ALWAYS {
            override fun toStatisticValue(): String {
                return Statistics.ParamValue.ALWAYS
            }

            override fun check(
                fragmentManager: FragmentManager,
                listener: NetworkPolicyListener,
                isDialogAllowed: Boolean
            ) {
                val nowInRoaming: Boolean = ConnectionState.isInRoaming
                val acceptedInRoaming: Boolean = Config.getMobileDataRoaming()
                if (nowInRoaming && !acceptedInRoaming) showDialog(
                    fragmentManager,
                    listener
                ) else listener.onResult(NetworkPolicy(true))
            }
        },
        NEVER {
            override fun toStatisticValue(): String {
                return Statistics.ParamValue.NEVER
            }

            override fun check(
                fragmentManager: FragmentManager,
                listener: NetworkPolicyListener,
                isDialogAllowed: Boolean
            ) {
                if (isDialogAllowed) showDialog(
                    fragmentManager,
                    listener
                ) else listener.onResult(NetworkPolicy(false))
            }
        },
        NOT_TODAY {
            override fun toStatisticValue(): String {
                return Statistics.ParamValue.NOT_TODAY
            }

            override fun check(
                fragmentManager: FragmentManager,
                listener: NetworkPolicyListener,
                isDialogAllowed: Boolean
            ) {
                if (isDialogAllowed) showDialog(
                    fragmentManager,
                    listener
                ) else showDialogIfNeeded(
                    fragmentManager,
                    listener,
                    NetworkPolicy(false)
                )
            }
        },
        TODAY {
            override fun toStatisticValue(): String {
                return Statistics.ParamValue.TODAY
            }

            override fun check(
                fragmentManager: FragmentManager,
                listener: NetworkPolicyListener,
                isDialogAllowed: Boolean
            ) {
                val nowInRoaming: Boolean = ConnectionState.isInRoaming
                val acceptedInRoaming: Boolean = Config.getMobileDataRoaming()
                if (nowInRoaming && !acceptedInRoaming) showDialog(
                    fragmentManager,
                    listener
                ) else showDialogIfNeeded(
                    fragmentManager,
                    listener,
                    NetworkPolicy(true)
                )
            }
        },
        NONE {
            override fun toStatisticValue(): String {
                throw UnsupportedOperationException("Not supported here $name")
            }
        };

        open fun check(
            fragmentManager: FragmentManager,
            listener: NetworkPolicyListener,
            isDialogAllowed: Boolean
        ) {
            showDialog(fragmentManager, listener)
        }
    }

    fun canUseNetwork(): Boolean {
        return mCanUseNetwork
    }

    interface NetworkPolicyListener {
        fun onResult(policy: NetworkPolicy)
    }

    companion object {
        const val NONE = -1
        private const val TAG_NETWORK_POLICY = "network_policy"

        @JvmStatic
        fun checkNetworkPolicy(
            fragmentManager: FragmentManager,
            listener: NetworkPolicyListener,
            isDialogAllowed: Boolean
        ) {
            if (ConnectionState.isWifiConnected) {
                listener.onResult(NetworkPolicy(true))
                return
            }
            if (!ConnectionState.isMobileConnected) {
                listener.onResult(NetworkPolicy(false))
                return
            }
            val type: Type = Config.getUseMobileDataSettings()
            type.check(fragmentManager, listener, isDialogAllowed)
        }

        @JvmStatic
        fun checkNetworkPolicy(
            fragmentManager: FragmentManager,
            listener: NetworkPolicyListener
        ) {
            checkNetworkPolicy(
                fragmentManager,
                listener,
                false
            )
        }

        @JvmStatic
        fun getCurrentNetworkUsageStatus(): Boolean {
            if (ConnectionState.isWifiConnected) return true
            if (!ConnectionState.isMobileConnected) return false
            val nowInRoaming: Boolean = ConnectionState.isInRoaming
            val acceptedInRoaming: Boolean = Config.getMobileDataRoaming()
            if (nowInRoaming && !acceptedInRoaming) return false
            val type: Type = Config.getUseMobileDataSettings()
            return type === Type.ALWAYS || type === Type.TODAY && isToday()
        }

        private fun isToday(): Boolean {
            val timestamp: Long = Config.getMobileDataTimeStamp()
            return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp) < 1
        }

        private fun showDialogIfNeeded(
            fragmentManager: FragmentManager,
            listener: NetworkPolicyListener,
            policy: NetworkPolicy
        ) {
            if (isToday()) {
                listener.onResult(policy)
                return
            }
            showDialog(fragmentManager, listener)
        }

        private fun showDialog(
            fragmentManager: FragmentManager,
            listener: NetworkPolicyListener
        ) {
            var dialog: StackedButtonDialogFragment? = fragmentManager
                .findFragmentByTag(TAG_NETWORK_POLICY) as StackedButtonDialogFragment?
            if (dialog != null) dialog.dismiss()
            dialog = StackedButtonDialogFragment()
            dialog.setListener(listener)
            dialog.show(
                fragmentManager,
                TAG_NETWORK_POLICY
            )
        }

        @JvmStatic
        fun newInstance(canUse: Boolean): NetworkPolicy {
            return NetworkPolicy(canUse)
        }
    }

}