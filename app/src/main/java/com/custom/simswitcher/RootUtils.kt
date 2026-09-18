package com.custom.simswitcher

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

    fun applyNetworkModes(sim1Mode: Int, sim2Mode: Int, toggleAirplane: Boolean): Boolean {
        val cmds = mutableListOf<String>()

        // 1. Standard AOSP Global Settings Keys
        cmds.add("settings put global preferred_network_mode1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode2 $sim2Mode")

        // 2. Samsung Multi-SIM & Slot Specific Keys
        cmds.add("settings put global preferred_network_mode_sub1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode_sub2 $sim2Mode")
        cmds.add("settings put global preferred_network_mode_sim1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode_sim2 $sim2Mode")
        cmds.add("settings put global preferred_network_mode_slot1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode_slot2 $sim2Mode")

        // 3. Samsung Primary Data SIM Routing (Hardware Transceiver Slot Mapping)
        if (sim1Mode == 11 || sim1Mode == 9 || sim1Mode == 12 || sim1Mode == 2) {
            // SIM 1 needs 3G/4G primary channel
            cmds.add("settings put global preferred_network_mode $sim1Mode")
            cmds.add("settings put global multi_sim_data_call 1")
            cmds.add("settings put global user_preferred_data_sub 1")
            cmds.add("settings put global user_preferred_sub1 1")
        } else if (sim2Mode == 9 || sim2Mode == 2 || sim2Mode == 11) {
            // SIM 2 needs 3G/4G primary channel
            cmds.add("settings put global preferred_network_mode $sim2Mode")
            cmds.add("settings put global multi_sim_data_call 2")
            cmds.add("settings put global user_preferred_data_sub 2")
            cmds.add("settings put global user_preferred_sub2 2")
        }

        // 4. Secure & System Database mirrors
        cmds.add("settings put secure preferred_network_mode1 $sim1Mode")
        cmds.add("settings put secure preferred_network_mode2 $sim2Mode")

        // 5. Toggle Airplane Mode to force modem re-attach & RIL read
        if (toggleAirplane) {
            cmds.add("settings put global airplane_mode_on 1")
            cmds.add("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true")
            cmds.add("sleep 4")
            cmds.add("settings put global airplane_mode_on 0")
            cmds.add("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false")
        }

        return executeRootCommands(*cmds.toTypedArray())
    }
}
