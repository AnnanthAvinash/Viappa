package avinash.app.audiocallapp.service

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import avinash.app.audiocallapp.data.repository.FcmTokenRepository
import avinash.app.audiocallapp.util.InAppNotificationBus
import avinash.app.audiocallapp.util.NotificationEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FcmService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")
        serviceScope.launch {
            fcmTokenRepository.saveTokenToFirestore(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received: ${message.data}")

        val title = message.data["title"] ?: message.notification?.title ?: "Vaippa"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val type = message.data["type"] ?: ""

        if (isAppInForeground()) {
            InAppNotificationBus.emit(NotificationEvent(title, body, type))
        } else {
            NotificationHelper.showConnectionNotification(
                this, title, body,
                notificationId = message.data["requestId"]?.hashCode() ?: System.currentTimeMillis().toInt()
            )
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        return appProcesses.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                it.processName == packageName
        }
    }
}
