package avinash.app.audiocallapp.feature

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface CallStateProvider {
    val isInActiveCall: StateFlow<Boolean>
    val activeCallRemoteUserId: StateFlow<String?>
}

class NoOpCallStateProvider : CallStateProvider {
    override val isInActiveCall: StateFlow<Boolean> = MutableStateFlow(false)
    override val activeCallRemoteUserId: StateFlow<String?> = MutableStateFlow(null)
}
