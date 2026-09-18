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
        cmds.add("settings put global preferred_network_mode1 $sim1Mode")
        cmds.add("settings put global preferred_network_mode2 $sim2Mode")

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
