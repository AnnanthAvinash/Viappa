package avinash.app.audiocallapp.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import avinash.app.audiocallapp.feature.NotificationConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalkieTalkieForegroundService : Service() {

    companion object {
        private const val TAG = "WT_SERVICE"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_START = "wt_action_start"
        const val ACTION_STOP = "wt_action_stop"

        fun startService(context: Context) {
            val intent = Intent(context, WalkieTalkieForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WalkieTalkieForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startWalkieTalkie()
            ACTION_STOP -> stopWalkieTalkie()
        }
        return START_STICKY
    }

    private fun startWalkieTalkie() {
        if (isRunning) return
        isRunning = true
        Log.d(TAG, "Starting Walkie-Talkie foreground service")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        acquireWakeLock()
    }

    private fun stopWalkieTalkie() {
        Log.d(TAG, "Stopping Walkie-Talkie foreground service")
        isRunning = false
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AudioCallApp:WalkieTalkieWakeLock"
            ).apply {
                acquire(10 * 60 * 60 * 1000L) // 10 hours max
            }
            Log.d(TAG, "Wake lock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released")
            }
        }
        wakeLock = null
    }

    private fun createNotification(): Notification {
        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: Intent()
        val pendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WalkieTalkieForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationConfig.WT_CHANNEL_ID)
            .setContentTitle("Walkie-Talkie Active")
            .setContentText("Listening for friends")
            .setSmallIcon(NotificationConfig.notificationIconRes)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        isRunning = false
        Log.d(TAG, "Service destroyed")
    }
}
