package avinash.app.audiocallapp.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import avinash.app.audiocallapp.feature.NotificationConfig
import avinash.app.audiocallapp.domain.usecase.CallUseCase
import avinash.app.audiocallapp.webrtc.NetworkChangeEvent
import avinash.app.audiocallapp.webrtc.NetworkChangeHandler
import avinash.app.audiocallapp.webrtc.NetworkType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallForegroundService : Service() {

    companion object {
        private const val TAG = "AudioCallApp"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_CALL = "action_start_call"
        const val ACTION_END_CALL = "action_end_call"
        const val EXTRA_REMOTE_USER_NAME = "extra_remote_user_name"
        const val EXTRA_IS_INCOMING = "extra_is_incoming"
        const val EXTRA_CALL_ID = "extra_call_id"

        fun startService(
            context: Context, 
            remoteUserName: String, 
            isIncoming: Boolean,
            callId: String? = null
        ) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_REMOTE_USER_NAME, remoteUserName)
                putExtra(EXTRA_IS_INCOMING, isIncoming)
                callId?.let { putExtra(EXTRA_CALL_ID, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_END_CALL
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var callUseCase: CallUseCase

    @Inject
    lateinit var networkChangeHandler: NetworkChangeHandler

    private val binder = LocalBinder()
    private var remoteUserName: String = ""
    private var callId: String? = null
    private var callStartTime: Long = 0L
    private var isCallActive = false
    private var currentNetworkType: NetworkType = NetworkType.UNKNOWN
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var networkObserverJob: kotlinx.coroutines.Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): CallForegroundService = this@CallForegroundService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                remoteUserName = intent.getStringExtra(EXTRA_REMOTE_USER_NAME) ?: "Unknown"
                callId = intent.getStringExtra(EXTRA_CALL_ID)
                val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)
                startForegroundCall(remoteUserName, isIncoming)
                startNetworkMonitoring()
            }
            ACTION_END_CALL -> {
                stopForegroundCall()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCall(userName: String, isIncoming: Boolean) {
        Log.d(TAG, "Starting foreground call with $userName")
        isCallActive = true
        callStartTime = System.currentTimeMillis()

        val notification = createCallNotification(userName, isIncoming)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundCall() {
        Log.d(TAG, "Stopping foreground call")
        isCallActive = false
        networkObserverJob?.cancel()
        performCleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun startNetworkMonitoring() {
        networkObserverJob?.cancel()
        networkObserverJob = serviceScope.launch {
            networkChangeHandler.observeNetworkChanges()
                .catch { e ->
                    Log.e(TAG, "Error observing network changes", e)
                }
                .collect { event ->
                    handleNetworkChange(event)
                }
        }
    }
    
    private fun handleNetworkChange(event: NetworkChangeEvent) {
        when (event) {
            is NetworkChangeEvent.NetworkAvailable -> {
                currentNetworkType = event.networkType
                updateNotificationWithNetworkStatus()
            }
            is NetworkChangeEvent.NetworkTypeChanged -> {
                Log.d(TAG, "Network type changed: ${event.from} -> ${event.to}")
                currentNetworkType = event.to
                updateNotificationWithNetworkStatus("Reconnecting...")
            }
            is NetworkChangeEvent.NetworkLost -> {
                updateNotificationWithNetworkStatus("Network lost")
            }
            is NetworkChangeEvent.NetworkUnavailable -> {
                updateNotificationWithNetworkStatus("No network")
            }
        }
    }
    
    private fun updateNotificationWithNetworkStatus(status: String? = null) {
        if (isCallActive) {
            val title = status ?: "Ongoing call"
            val networkInfo = when (currentNetworkType) {
                NetworkType.WIFI -> "Wi-Fi"
                NetworkType.MOBILE -> "Mobile data"
                NetworkType.ETHERNET -> "Ethernet"
                NetworkType.VPN -> "VPN"
                else -> ""
            }
            val text = if (networkInfo.isNotEmpty()) {
                "Call with $remoteUserName • $networkInfo"
            } else {
                "Call with $remoteUserName"
            }
            
            val notification = createCallNotification(remoteUserName, false).let { base ->
                NotificationCompat.Builder(this, NotificationConfig.CALL_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(NotificationConfig.notificationIconRes)
                    .setOngoing(true)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setUsesChronometer(true)
                    .setWhen(callStartTime)
                    .build()
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
    
    private fun performCleanup() {
        serviceScope.launch {
            try {
                callId?.let { id ->
                    Log.d(TAG, "Cleaning up call: $id")
                    // End the call
                    callUseCase.endCall(id)
                    // Wait a bit for status update
                    kotlinx.coroutines.delay(500)
                    // Cleanup Firestore documents
                    callUseCase.cleanupCall(id)
                    Log.d(TAG, "Call cleanup completed: $id")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

    private fun createCallNotification(userName: String, isIncoming: Boolean): Notification {
        val contentIntent = (packageManager.getLaunchIntentForPackage(packageName) ?: Intent()).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "active_call")
            putExtra("remote_user_name", userName)
            callId?.let { putExtra("call_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endCallIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getService(
            this,
            1,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isIncoming) "Incoming call" else "Ongoing call"
        val text = "Call with $userName"

        return NotificationCompat.Builder(this, NotificationConfig.CALL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(NotificationConfig.notificationIconRes)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endCallPendingIntent
            )
            .setUsesChronometer(true)
            .setWhen(callStartTime)
            .build()
    }

    fun updateCallStatus(status: String) {
        if (isCallActive) {
            val notification = NotificationCompat.Builder(this, NotificationConfig.CALL_CHANNEL_ID)
                .setContentTitle(status)
                .setContentText("Call with $remoteUserName")
                .setSmallIcon(NotificationConfig.notificationIconRes)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setUsesChronometer(true)
                .setWhen(callStartTime)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        // Guarantee cleanup even on unexpected termination
        if (isCallActive) {
            Log.d(TAG, "Service destroyed while call active - performing cleanup")
            performCleanup()
        }
        
        networkObserverJob?.cancel()
        networkChangeHandler.stop()
        serviceScope.cancel()
    }
}
