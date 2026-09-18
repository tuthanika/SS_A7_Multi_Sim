package com.custom.simswitcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class SimRotatorService : Service() {

    private lateinit var prefsManager: PreferenceManager
    private val handler = Handler(Looper.getMainLooper())
    private var rotationRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferenceManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                prefsManager.isServiceRunning = true
                startForeground(NOTIFICATION_ID, buildNotification("Bắt đầu dịch vụ..."))
                startRotationLoop()
            }
            ACTION_STOP -> {
                stopRotationLoop()
                prefsManager.isServiceRunning = false
                stopForeground(true)
                stopSelf()
            }
            ACTION_SWITCH_SIM1 -> {
                switchToPhase(1)
            }
            ACTION_SWITCH_SIM2 -> {
                switchToPhase(2)
            }
            ACTION_TOGGLE -> {
                val nextPhase = if (prefsManager.currentPhase == 1) 2 else 1
                switchToPhase(nextPhase)
            }
        }

        return START_STICKY
    }

    private fun startRotationLoop() {
        stopRotationLoop()
        runPhaseStep()
    }

    private fun stopRotationLoop() {
        rotationRunnable?.let { handler.removeCallbacks(it) }
        rotationRunnable = null
    }

    private fun runPhaseStep() {
        val currentPhase = prefsManager.currentPhase
        applyPhase(currentPhase)

        if (!prefsManager.autoRotationEnabled) {
            Log.d(TAG, "Auto rotation disabled. Remaining on Phase $currentPhase")
            return
        }

        val durationMs = if (currentPhase == 1) {
            prefsManager.sim1DurationMinutes * 60 * 1000L
        } else {
            prefsManager.sim2DurationMinutes * 60 * 1000L
        }

        Log.d(TAG, "Phase $currentPhase active. Next switch in ${durationMs / 1000}s")

        rotationRunnable = Runnable {
            val nextPhase = if (currentPhase == 1) 2 else 1
            prefsManager.currentPhase = nextPhase
            runPhaseStep()
        }

        rotationRunnable?.let { handler.postDelayed(it, durationMs) }
    }

    private fun switchToPhase(phase: Int) {
        prefsManager.currentPhase = phase
        stopRotationLoop()
        runPhaseStep()
    }

    private fun applyPhase(phase: Int) {
        val sim1Mode: Int
        val sim2Mode: Int
        val statusText: String

        if (phase == 1) {
            sim1Mode = prefsManager.sim1Mode
            sim2Mode = 1 // 2G Only
            statusText = "SIM1: 4G (mode $sim1Mode) | SIM2: 2G"
        } else {
            sim1Mode = 1 // 2G Only
            sim2Mode = prefsManager.sim2Mode
            statusText = "SIM1: 2G | SIM2: 3G/4G (mode $sim2Mode)"
        }

        Log.d(TAG, "Applying Phase $phase: $statusText")

        Thread {
            RootUtils.applyNetworkModesFast(applicationContext, sim1Mode, sim2Mode, prefsManager.useShizuku)
            handler.post {
                updateNotification(statusText)
                SimTileService.updateTileState(applicationContext, phase)
            }
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SIM Network Switcher",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị trạng thái xoay tua SIM"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sim1Intent = PendingIntent.getService(
            this, 1, Intent(this, SimRotatorService::class.java).apply { action = ACTION_SWITCH_SIM1 },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sim2Intent = PendingIntent.getService(
            this, 2, Intent(this, SimRotatorService::class.java).apply { action = ACTION_SWITCH_SIM2 },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SIM Network Switcher")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_sim_card)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_sim_card, "SIM1 4G", sim1Intent)
            .addAction(R.drawable.ic_sim_card, "SIM2 3G/4G", sim2Intent)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRotationLoop()
        prefsManager.isServiceRunning = false
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SimRotatorService"
        const val CHANNEL_ID = "sim_rotator_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.custom.simswitcher.START"
        const val ACTION_STOP = "com.custom.simswitcher.STOP"
        const val ACTION_SWITCH_SIM1 = "com.custom.simswitcher.SIM1"
        const val ACTION_SWITCH_SIM2 = "com.custom.simswitcher.SIM2"
        const val ACTION_TOGGLE = "com.custom.simswitcher.TOGGLE"

        fun startService(context: Context) {
            val intent = Intent(context, SimRotatorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SimRotatorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
