package avinash.app.audiocallapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import avinash.app.audiocallapp.feature.NotificationConfig
import avinash.app.audiocallapp.service.NotificationHelper
import avinash.app.audiocallapp.webrtc.WebRTCClient
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AudioCallApp : Application() {

    companion object {
        private const val TAG = "AudioCallApp"
    }

    override fun onCreate() {
        super.onCreate()
        NotificationConfig.notificationIconRes = R.drawable.ic_launcher_foreground
        createNotificationChannels()
        NotificationHelper.createConnectionChannel(this)
        configureTurnServers()
    }

    private fun configureTurnServers() {
        val server = BuildConfig.TURN_SERVER
        val username = BuildConfig.TURN_USERNAME
        val password = BuildConfig.TURN_PASSWORD

        if (server.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
            WebRTCClient.setTurnServers(server, username, password)
            Log.d(TAG, "[TURN] Configured: $server")
        } else {
            Log.w(TAG, "[TURN] Not configured - calls may fail on different networks")
            Log.w(TAG, "[TURN] Add TURN_SERVER, TURN_USERNAME, TURN_PASSWORD to local.properties")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val callChannel = NotificationChannel(
                NotificationConfig.CALL_CHANNEL_ID,
                "Audio Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for ongoing audio calls"
                setSound(null, null)
                enableVibration(true)
            }

            val wtChannel = NotificationChannel(
                NotificationConfig.WT_CHANNEL_ID,
                "Walkie-Talkie",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Walkie-Talkie background service"
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(wtChannel)
        }
    }
}
