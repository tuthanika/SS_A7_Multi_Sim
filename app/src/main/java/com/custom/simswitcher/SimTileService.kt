package com.custom.simswitcher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class SimTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val prefsManager = PreferenceManager(applicationContext)

        // Send Intent to toggle SIM mode immediately via Service
        val intent = Intent(applicationContext, SimRotatorService::class.java).apply {
            action = SimRotatorService.ACTION_TOGGLE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val prefsManager = PreferenceManager(applicationContext)
        val phase = prefsManager.currentPhase

        if (phase == 1) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "SIM1 (4G Only)"
            tile.subtitle = "Chạm để đổi SIM2"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "SIM2 (3G/4G)"
            tile.subtitle = "Chạm để đổi SIM1"
        }

        tile.updateTile()
    }

    companion object {
        fun updateTileState(context: Context, phase: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestListeningState(
                    context,
                    android.content.ComponentName(context, SimTileService::class.java)
                )
            }
        }
    }
}
