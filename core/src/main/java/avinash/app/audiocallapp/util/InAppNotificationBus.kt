package avinash.app.audiocallapp.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class NotificationEvent(
    val title: String,
    val body: String,
    val type: String = ""
)

object InAppNotificationBus {
    private val _events = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    fun emit(event: NotificationEvent) {
        _events.tryEmit(event)
    }
}
