package com.custom.simswitcher

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import rikka.shizuku.Shizuku
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

    fun isShizukuAvailable(): Boolean {
        return try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    true
                } else {
                    Shizuku.requestPermission(1002)
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku check failed", e)
            false
        }
    }

    fun executeCommands(vararg commands: String): Boolean {
        // First try Direct Root su execution
        if (isRootAvailable()) {
            val success = executeRootCommands(*commands)
            if (success) return true
        }

        // Fallback to Shizuku execution if available
        if (isShizukuAvailable()) {
            return executeShizukuCommands(*commands)
        }

        return false
    }

    private fun executeRootCommands(vararg commands: String): Boolean {
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

    private fun executeShizukuCommands(vararg commands: String): Boolean {
        return try {
            val fullCmd = commands.joinToString(" && ")
            Log.d(TAG, "Running Shizuku cmd: $fullCmd")
            val process = Shizuku.newProcess(arrayOf("sh", "-c", fullCmd), null, null)
            val result = process.waitFor()
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute Shizuku commands", e)
            false
        }
    }

    fun applyNetworkModesFast(context: Context, sim1Mode: Int, sim2Mode: Int): Boolean {
        val sub1Id = getSubscriptionIdForSlot(context, 0) ?: 1
        val sub2Id = getSubscriptionIdForSlot(context, 1) ?: 2

        Log.d(TAG, "Detected SubIDs: SIM1(Slot 0) -> SubId $sub1Id, SIM2(Slot 1) -> SubId $sub2Id")

        val cmds = mutableListOf<String>()

        // 1. Update Samsung Stock ROM & AOSP Global Settings Keys
        cmds.add("settings put global preferred_network_mode1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode2 $sim2Mode")
        cmds.add("settings put global preferred_network_mode_sub1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode_sub2 $sim2Mode")
        cmds.add("settings put global preferred_network_mode_slot1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode_slot2 $sim2Mode")
        cmds.add("settings put global preferred_network_mode $sim1Mode")

        // 2. Primary Data SIM hardware slot switch
        if (sim1Mode == 11 || sim1Mode == 9 || sim1Mode == 2) {
            cmds.add("settings put global multi_sim_data_call $sub1Id")
            cmds.add("settings put global user_preferred_data_sub $sub1Id")
        } else if (sim2Mode == 9 || sim2Mode == 2 || sim2Mode == 11) {
            cmds.add("settings put global multi_sim_data_call $sub2Id")
            cmds.add("settings put global user_preferred_data_sub $sub2Id")
        }

        // 3. Telephony CLI commands for detected Sub IDs
        cmds.add("cmd telephony set-preferred-network-type $sub1Id $sim1Mode 2>/dev/null || true")
        cmds.add("cmd telephony set-preferred-network-type $sub2Id $sim2Mode 2>/dev/null || true")
        cmds.add("cmd telephony set-preferred-network-type 0 $sim1Mode 2>/dev/null || true")
        cmds.add("cmd telephony set-preferred-network-type 1 $sim1Mode 2>/dev/null || true")

        // 4. Samsung Stock RIL Daemon Refresh (Reloads SecRIL settings without Airplane Mode)
        cmds.add("pkill -f rild 2>/dev/null || killall rild 2>/dev/null || true")

        val cmdResult = executeCommands(*cmds.toTypedArray())

        // 5. Invoke Reflection API
        setNetworkTypeViaReflection(context, sub1Id, sim1Mode)
        setNetworkTypeViaReflection(context, sub2Id, sim2Mode)

        return cmdResult
    }

    private fun getSubscriptionIdForSlot(context: Context, slotIndex: Int): Int? {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val activeList = sm?.activeSubscriptionInfoList
            activeList?.firstOrNull { it.simSlotIndex == slotIndex }?.subscriptionId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subId for slot $slotIndex", e)
            null
        }
    }

    private fun setNetworkTypeViaReflection(context: Context, subId: Int, networkType: Int): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            try {
                val method = tm.javaClass.getMethod("setPreferredNetworkType", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                method.isAccessible = true
                val res = method.invoke(tm, subId, networkType) as? Boolean
                if (res == true) return true
            } catch (_: Exception) {}

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
