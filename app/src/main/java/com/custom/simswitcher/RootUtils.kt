package com.custom.simswitcher

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import java.io.DataOutputStream

object RootUtils {
    private const val TAG = "RootUtils"

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Root check failed", e)
            false
        }
    }

    fun executeRootCommands(vararg commands: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (cmd in commands) {
                Log.d(TAG, "Running root cmd: $cmd")
                os.writeBytes("$cmd\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            val result = process.waitFor()
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute root commands", e)
            false
        }
    }

    fun applyNetworkModesFast(context: Context, sim1Mode: Int, sim2Mode: Int): Boolean {
        val cmds = mutableListOf<String>()

        // 1. Update Global Settings Database
        cmds.add("settings put global preferred_network_mode1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode2 $sim2Mode")
        cmds.add("settings put global preferred_network_mode_sub1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode_sub2 $sim2Mode")
        cmds.add("settings put global preferred_network_mode_sim1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode_sim2 $sim2Mode")

        // 2. Direct Telephony Binder / RIL commands for instant baseband mode switch (No Airplane mode needed)
        // Try cmd telephony set-preferred-network-type (Android 7.0+)
        cmds.add("cmd telephony set-preferred-network-type 1 $sim1Mode 2>/dev/null || true")
        cmds.add("cmd telephony set-preferred-network-type 2 $sim2Mode 2>/dev/null || true")
        cmds.add("cmd telephony set-preferred-network-type 0 $sim1Mode 2>/dev/null || true")

        // Try service call phone ITelephony setPreferredNetworkType (common Nougat transaction codes: 94, 104, 107)
        cmds.add("service call phone 94 i32 1 i32 $sim1Mode 2>/dev/null || true")
        cmds.add("service call phone 94 i32 2 i32 $sim2Mode 2>/dev/null || true")
        cmds.add("service call phone 104 i32 1 i32 $sim1Mode 2>/dev/null || true")
        cmds.add("service call phone 104 i32 2 i32 $sim2Mode 2>/dev/null || true")

        // 3. Notify Telephony Framework of network mode modification
        cmds.add("am broadcast -a android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE 2>/dev/null || true")

        val rootResult = executeRootCommands(*cmds.toTypedArray())

        // Also attempt reflection via TelephonyManager
        setNetworkTypeViaReflection(context, 1, sim1Mode)
        setNetworkTypeViaReflection(context, 2, sim2Mode)

        return rootResult
    }

    private fun setNetworkTypeViaReflection(context: Context, subId: Int, networkType: Int): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            // Attempt Method 1: tm.setPreferredNetworkType(subId, networkType)
            try {
                val method = tm.javaClass.getMethod("setPreferredNetworkType", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                method.isAccessible = true
                val res = method.invoke(tm, subId, networkType) as? Boolean
                if (res == true) return true
            } catch (_: Exception) {}

            // Attempt Method 2: tm.createForSubscriptionId(subId).setPreferredNetworkType(networkType)
            try {
                val createMethod = tm.javaClass.getMethod("createForSubscriptionId", Int::class.javaPrimitiveType)
                val subTm = createMethod.invoke(tm, subId) as? TelephonyManager
                if (subTm != null) {
                    val method = subTm.javaClass.getMethod("setPreferredNetworkType", Int::class.javaPrimitiveType)
                    method.isAccessible = true
                    val res = method.invoke(subTm, networkType) as? Boolean
                    if (res == true) return true
                }
            } catch (_: Exception) {}

            false
        } catch (e: Exception) {
            Log.e(TAG, "Reflection setPreferredNetworkType failed for sub $subId", e)
            false
        }
    }
}
