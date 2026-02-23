package avinash.app.audiocallapp.call

import android.content.Context
import android.util.Log
import avinash.app.audiocallapp.data.model.CallSignal
import avinash.app.audiocallapp.data.model.CallStatus
import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.data.repository.CallHistoryRepository
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.usecase.CallUseCase
import avinash.app.audiocallapp.feature.CallStateProvider
import avinash.app.audiocallapp.feature.NotificationConfig
import avinash.app.audiocallapp.service.CallForegroundService
import avinash.app.audiocallapp.webrtc.NetworkChangeEvent
import avinash.app.audiocallapp.webrtc.NetworkChangeHandler
import avinash.app.audiocallapp.webrtc.NetworkType
import avinash.app.audiocallapp.webrtc.WebRTCClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.PeerConnection
import javax.inject.Inject
import javax.inject.Singleton

enum class CallState {
    IDLE,
    OUTGOING,
    INCOMING,
    CONNECTING,
    CONNECTED,
    ENDED,
    FAILED
}

data class CallUiState(
    val callState: CallState = CallState.IDLE,
    val callId: String? = null,
    val remoteUserId: String = "",
    val remoteUserName: String = "",
    val isCaller: Boolean = true,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val callDuration: Long = 0L,
    val errorMessage: String? = null,
    val isReconnecting: Boolean = false,
    val networkType: NetworkType = NetworkType.UNKNOWN
)

@Singleton
class CallManager @Inject constructor(
    private val callUseCase: CallUseCase,
    private val authRepository: AuthRepository,
    private val webRTCClient: WebRTCClient,
    private val networkChangeHandler: NetworkChangeHandler,
    private val callHistoryRepository: CallHistoryRepository,
    private val context: Context
) : CallStateProvider {
    companion object {
        private const val TAG = "MOBILETAGFILTER"
        private const val RECONNECTION_TIMEOUT_MS = 30_000L
        private const val ICE_CHECKING_TIMEOUT_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _incomingCall = MutableStateFlow<CallSignal?>(null)
    val incomingCall: StateFlow<CallSignal?> = _incomingCall.asStateFlow()

    private val _callUiState = MutableStateFlow(CallUiState())
    val callUiState: StateFlow<CallUiState> = _callUiState.asStateFlow()

    private val _isInActiveCall = MutableStateFlow(false)
    override val isInActiveCall: StateFlow<Boolean> = _isInActiveCall.asStateFlow()

    private val _activeCallRemoteUserId = MutableStateFlow<String?>(null)
    override val activeCallRemoteUserId: StateFlow<String?> = _activeCallRemoteUserId.asStateFlow()

    private var incomingCallObserverJob: Job? = null
    private var callObserverJob: Job? = null
    private var iceCandidateObserverJob: Job? = null
    private var durationJob: Job? = null
    private var networkObserverJob: Job? = null
    private var reconnectionTimeoutJob: Job? = null
    private var iceCheckingTimeoutJob: Job? = null

    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private val processedCandidateIds = mutableSetOf<String>()
    private var isRemoteDescriptionSet = false
    private var isCallEnding = false
    private var previousNetworkType: NetworkType = NetworkType.UNKNOWN
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        webRTCClient.initialize()
        startNetworkMonitoring()
    }

    fun startObservingIncomingCalls(userId: String) {
        incomingCallObserverJob?.cancel()
        incomingCallObserverJob = scope.launch {
            Log.d(TAG, "[INCOMING] Listening for calls (calleeId: $userId)")
            callUseCase.observeIncomingCalls(userId)
                .catch { e -> Log.e(TAG, "[INCOMING] Observer error: ${e.message}") }
                .collect { call ->
                    if (call != null) {
                        Log.d(TAG, "[INCOMING] Call received: ${call.callerName} (${call.callerId}), currentState=${_callUiState.value.callState}")
                    }
                    _incomingCall.value = call
                }
        }
    }

    fun stopObservingIncomingCalls() {
        incomingCallObserverJob?.cancel()
        _incomingCall.value = null
    }

    // --- Outgoing Call ---

    fun initiateOutgoingCall(calleeId: String, calleeName: String) {
        resetState()
        scope.launch {
            val currentUser = authRepository.getCurrentUser() ?: run {
                Log.e(TAG, "[OUTGOING] User not authenticated")
                _callUiState.update {
                    it.copy(callState = CallState.FAILED, errorMessage = "User not authenticated")
                }
                return@launch
            }

            Log.d(TAG, "[OUTGOING] Starting call to $calleeName ($calleeId)")
            _callUiState.update {
                it.copy(
                    callState = CallState.OUTGOING,
                    remoteUserId = calleeId,
                    remoteUserName = calleeName,
                    isCaller = true,
                    networkType = networkChangeHandler.getCurrentNetworkType()
                )
            }
            _isInActiveCall.value = true
            _activeCallRemoteUserId.value = calleeId
            _incomingCall.value = null

            try {
                Log.d(TAG, "[OUTGOING] Creating peer connection...")
                webRTCClient.createPeerConnection(
                    onIceCandidate = { candidate ->
                        scope.launch {
                            val callId = _callUiState.value.callId
                            if (callId != null) {
                                callUseCase.addCallerIceCandidate(
                                    callId,
                                    IceCandidate(candidate.sdp, candidate.sdpMid ?: "", candidate.sdpMLineIndex)
                                )
                            }
                        }
                    },
                    onConnectionStateChange = { state ->
                        Log.d(TAG, "[OUTGOING] Connection state callback: $state")
                        handleConnectionStateChange(state)
                    },
                    onRemoteTrack = { track ->
                        Log.d(TAG, "[OUTGOING] Remote audio track received: ${track.kind()}")
                    }
                )

                var offerCreated = false
                webRTCClient.createOffer { sdp ->
                    offerCreated = true
                    scope.launch {
                        try {
                            val offer = SessionDescription(sdp.type.canonicalForm(), sdp.description)
                            val result = callUseCase.initiateCall(
                                currentUser.uniqueId, calleeId,
                                currentUser.displayName, calleeName, offer
                            )
                            result.fold(
                                onSuccess = { callId ->
                                    Log.d(TAG, "Call created, callId: $callId")
                                    _callUiState.update { it.copy(callId = callId) }
                                    try {
                                        CallForegroundService.startService(context, calleeName, false, callId)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to start foreground service", e)
                                    }
                                    observeCall(callId)
                                    observeCalleeIceCandidates(callId)
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Failed to create call", error)
                                    _callUiState.update {
                                        it.copy(callState = CallState.FAILED, errorMessage = error.message)
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing offer", e)
                            _callUiState.update {
                                it.copy(callState = CallState.FAILED, errorMessage = "Error creating offer: ${e.message}")
                            }
                        }
                    }
                }

                scope.launch {
                    delay(10000)
                    if (!offerCreated) {
                        _callUiState.update {
                            it.copy(callState = CallState.FAILED, errorMessage = "Call setup timed out.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating peer connection", e)
                _callUiState.update {
                    it.copy(callState = CallState.FAILED, errorMessage = "Error setting up call: ${e.message}")
                }
            }
        }
    }

    // --- Incoming Call ---

    fun acceptIncomingCall() {
        val call = _incomingCall.value ?: return
        acceptCall(call.callId, call.callerId, call.callerName)
    }

    private fun acceptCall(callId: String, callerId: String, callerName: String) {
        resetState()
        _incomingCall.value = null
        scope.launch {
            Log.d(TAG, "[INCOMING] Answering call from $callerName ($callerId)")
            _callUiState.update {
                it.copy(
                    callState = CallState.INCOMING,
                    callId = callId,
                    remoteUserId = callerId,
                    remoteUserName = callerName,
                    isCaller = false,
                    networkType = networkChangeHandler.getCurrentNetworkType()
                )
            }
            _isInActiveCall.value = true
            _activeCallRemoteUserId.value = callerId

            webRTCClient.createPeerConnection(
                onIceCandidate = { candidate ->
                    scope.launch {
                        callUseCase.addCalleeIceCandidate(
                            callId,
                            IceCandidate(candidate.sdp, candidate.sdpMid ?: "", candidate.sdpMLineIndex)
                        )
                    }
                },
                onConnectionStateChange = { state ->
                    Log.d(TAG, "[INCOMING] Connection state callback: $state")
                    handleConnectionStateChange(state)
                },
                onRemoteTrack = { track ->
                    Log.d(TAG, "[INCOMING] Remote audio track received: ${track.kind()}")
                }
            )

            CallForegroundService.startService(context, callerName, true, callId)
            observeCall(callId)
            observeCallerIceCandidates(callId)
        }
    }

    suspend fun rejectIncomingCall() {
        val call = _incomingCall.value ?: return
        Log.d(TAG, "[INCOMING] Rejecting call: ${call.callId}")
        callUseCase.rejectCall(call.callId)
        try {
            callHistoryRepository.insertCallRecord(
                remoteUserId = call.callerId,
                remoteName = call.callerName,
                type = "missed",
                durationSeconds = 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save missed call", e)
        }
        _incomingCall.value = null
        Log.d(TAG, "[INCOMING] Call rejected")
    }

    fun navigatedToCallScreen() {
        _incomingCall.value = null
    }

    // --- Active Call Controls ---

    fun toggleMute() {
        val newState = !_callUiState.value.isMuted
        webRTCClient.setMicrophoneMuted(newState)
        _callUiState.update { it.copy(isMuted = newState) }
    }

    fun toggleSpeaker() {
        val newState = !_callUiState.value.isSpeakerOn
        webRTCClient.setSpeakerEnabled(newState)
        _callUiState.update { it.copy(isSpeakerOn = newState) }
    }

    fun endCall() {
        if (isCallEnding) {
            Log.d(TAG, "endCall() already in progress, skipping")
            return
        }
        isCallEnding = true
        val prefix = if (_callUiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        Log.d(TAG, "$prefix endCall() started")
        val state = _callUiState.value
        scope.launch {
            val callId = state.callId
            if (callId != null) {
                Log.d(TAG, "$prefix Writing ENDED status to Firestore")
                callUseCase.endCall(callId)
                delay(1500)
                Log.d(TAG, "$prefix Deleting call document")
                callUseCase.cleanupCall(callId)
            }

            val callType = if (state.isCaller) "outgoing" else "incoming"
            try {
                callHistoryRepository.insertCallRecord(
                    remoteUserId = state.remoteUserId,
                    remoteName = state.remoteUserName,
                    type = callType,
                    durationSeconds = state.callDuration
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save call history", e)
            }

            Log.d(TAG, "$prefix Stopping service and cleaning up")
            durationJob?.cancel()
            CallForegroundService.stopService(context)
            cleanupWebRTC()
            _callUiState.update { it.copy(callState = CallState.ENDED) }
            _isInActiveCall.value = false
            _activeCallRemoteUserId.value = null
            Log.d(TAG, "$prefix endCall() complete")
        }
    }

    fun resetState() {
        isCallEnding = false
        isRemoteDescriptionSet = false
        pendingIceCandidates.clear()
        processedCandidateIds.clear()
        callObserverJob?.cancel()
        iceCandidateObserverJob?.cancel()
        durationJob?.cancel()
        reconnectionTimeoutJob?.cancel()
        iceCheckingTimeoutJob?.cancel()
        _callUiState.value = CallUiState()
        _isInActiveCall.value = false
        _activeCallRemoteUserId.value = null
        scope.launch { authRepository.updateUserStatus(UserStatus.ONLINE) }
    }

    // --- Firestore Observers ---

    private fun observeCall(callId: String) {
        callObserverJob?.cancel()
        callObserverJob = scope.launch {
            val prefix = if (_callUiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
            Log.d(TAG, "$prefix [OBSERVE] Starting observer for callId=$callId")
            callUseCase.observeCall(callId)
                .catch { e -> Log.e(TAG, "$prefix [OBSERVE] Flow error: ${e.message}") }
                .collect { call ->
                    Log.d(TAG, "$prefix [OBSERVE] Snapshot: status=${call?.status}, isCallEnding=$isCallEnding, currentState=${_callUiState.value.callState}")

                    if (call == null) {
                        Log.w(TAG, "$prefix [OBSERVE] Document DELETED - ending call")
                        if (!isCallEnding) {
                            isCallEnding = true
                            durationJob?.cancel()
                            CallForegroundService.stopService(context)
                            cleanupWebRTC()
                            authRepository.updateUserStatus(UserStatus.ONLINE)
                        }
                        _callUiState.update { it.copy(callState = CallState.ENDED) }
                        _isInActiveCall.value = false
                        _activeCallRemoteUserId.value = null
                        return@collect
                    }

                    when (call.status) {
                        CallStatus.RINGING -> {
                            if (!_callUiState.value.isCaller && call.offer != null && !isRemoteDescriptionSet) {
                                Log.d(TAG, "[INCOMING] Processing offer (${call.offer.sdp.length} bytes)...")
                                val remoteSdp = org.webrtc.SessionDescription(
                                    org.webrtc.SessionDescription.Type.OFFER, call.offer.sdp
                                )
                                webRTCClient.setRemoteDescription(remoteSdp) {
                                    isRemoteDescriptionSet = true
                                    processPendingIceCandidates()
                                    webRTCClient.createAnswer { answerSdp ->
                                        scope.launch {
                                            val answer = SessionDescription(answerSdp.type.canonicalForm(), answerSdp.description)
                                            callUseCase.answerCall(callId, answer)
                                            Log.d(TAG, "[INCOMING] Answer sent, state -> CONNECTING")
                                            _callUiState.update { it.copy(callState = CallState.CONNECTING) }
                                        }
                                    }
                                }
                            }
                        }
                        CallStatus.ACCEPTED -> {
                            if (_callUiState.value.isCaller && call.answer != null && !isRemoteDescriptionSet) {
                                val remoteSdp = org.webrtc.SessionDescription(
                                    org.webrtc.SessionDescription.Type.ANSWER, call.answer.sdp
                                )
                                webRTCClient.setRemoteDescription(remoteSdp) {
                                    isRemoteDescriptionSet = true
                                    processPendingIceCandidates()
                                }
                            }
                            _callUiState.update { it.copy(callState = CallState.CONNECTING) }
                        }
                        CallStatus.REJECTED -> {
                            _callUiState.update { it.copy(callState = CallState.ENDED, errorMessage = "Call was rejected") }
                            _isInActiveCall.value = false
                            _activeCallRemoteUserId.value = null
                        }
                        CallStatus.ENDED -> {
                            Log.d(TAG, "$prefix [OBSERVE] Status=ENDED from remote")
                            if (!isCallEnding) {
                                isCallEnding = true
                                durationJob?.cancel()
                                CallForegroundService.stopService(context)
                                cleanupWebRTC()
                            }
                            _callUiState.update { it.copy(callState = CallState.ENDED) }
                            _isInActiveCall.value = false
                            _activeCallRemoteUserId.value = null
                        }
                    }
                }
        }
    }

    private fun observeCallerIceCandidates(callId: String) {
        iceCandidateObserverJob?.cancel()
        iceCandidateObserverJob = scope.launch {
            callUseCase.observeCallerIceCandidates(callId).collect { candidates ->
                candidates.forEach { addIceCandidate(it) }
            }
        }
    }

    private fun observeCalleeIceCandidates(callId: String) {
        iceCandidateObserverJob?.cancel()
        iceCandidateObserverJob = scope.launch {
            callUseCase.observeCalleeIceCandidates(callId).collect { candidates ->
                candidates.forEach { addIceCandidate(it) }
            }
        }
    }

    // --- ICE Candidates ---

    private fun addIceCandidate(candidate: IceCandidate) {
        val candidateId = "${candidate.sdpMid}:${candidate.sdpMLineIndex}:${candidate.candidate.hashCode()}"
        if (candidateId in processedCandidateIds) return
        processedCandidateIds.add(candidateId)

        if (isRemoteDescriptionSet) {
            webRTCClient.addIceCandidate(
                org.webrtc.IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
            )
        } else {
            pendingIceCandidates.add(candidate)
        }
    }

    private fun processPendingIceCandidates() {
        if (pendingIceCandidates.isNotEmpty()) {
            Log.d(TAG, "Processing ${pendingIceCandidates.size} pending ICE candidates")
        }
        pendingIceCandidates.forEach { candidate ->
            webRTCClient.addIceCandidate(
                org.webrtc.IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
            )
        }
        pendingIceCandidates.clear()
    }

    // --- Connection State ---

    private fun handleConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        val prefix = if (_callUiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        Log.d(TAG, "$prefix [ICE] Connection state: $state")
        when (state) {
            PeerConnection.PeerConnectionState.NEW -> {}
            PeerConnection.PeerConnectionState.CONNECTING -> {
                startIceCheckingTimeout()
                _callUiState.update { it.copy(callState = CallState.CONNECTING) }
            }
            PeerConnection.PeerConnectionState.CONNECTED -> {
                Log.d(TAG, "$prefix [ICE] CONNECTED - call active!")
                iceCheckingTimeoutJob?.cancel()
                reconnectionTimeoutJob?.cancel()
                _callUiState.update { it.copy(callState = CallState.CONNECTED, isReconnecting = false) }
                startCallDurationTimer()
            }
            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                Log.w(TAG, "$prefix [ICE] DISCONNECTED - waiting for Firestore signal")
            }
            PeerConnection.PeerConnectionState.FAILED -> {
                iceCheckingTimeoutJob?.cancel()
                _callUiState.update {
                    it.copy(callState = CallState.FAILED, errorMessage = "Connection failed. TURN server may be required.")
                }
            }
            PeerConnection.PeerConnectionState.CLOSED -> {
                iceCheckingTimeoutJob?.cancel()
                _callUiState.update { it.copy(callState = CallState.ENDED) }
                _isInActiveCall.value = false
                _activeCallRemoteUserId.value = null
            }
            else -> {}
        }
    }

    private fun startIceCheckingTimeout() {
        iceCheckingTimeoutJob?.cancel()
        iceCheckingTimeoutJob = scope.launch {
            delay(ICE_CHECKING_TIMEOUT_MS)
            if (_callUiState.value.callState == CallState.CONNECTING) {
                _callUiState.update {
                    it.copy(callState = CallState.FAILED, errorMessage = "Connection timeout. TURN server may be required.")
                }
                endCall()
            }
        }
    }

    private fun startCallDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            var duration = 0L
            while (true) {
                _callUiState.update { it.copy(callDuration = duration) }
                delay(1000)
                duration++
            }
        }
    }

    // --- Network Monitoring ---

    private fun startNetworkMonitoring() {
        networkObserverJob?.cancel()
        networkObserverJob = scope.launch {
            networkChangeHandler.observeNetworkChanges()
                .catch { e -> Log.e(TAG, "Error observing network changes", e) }
                .collect { event -> handleNetworkChange(event) }
        }
    }

    private fun handleNetworkChange(event: NetworkChangeEvent) {
        val prefix = if (_callUiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        when (event) {
            is NetworkChangeEvent.NetworkAvailable -> {
                _callUiState.update { it.copy(networkType = event.networkType) }
                previousNetworkType = event.networkType
            }
            is NetworkChangeEvent.NetworkTypeChanged -> {
                Log.d(TAG, "$prefix Network changed: ${event.from} -> ${event.to}")
                _callUiState.update { it.copy(networkType = event.to, isReconnecting = true) }
                previousNetworkType = event.to
                if (_callUiState.value.callState in listOf(CallState.CONNECTING, CallState.CONNECTED)) {
                    restartIceOnNetworkChange()
                }
            }
            is NetworkChangeEvent.NetworkLost,
            is NetworkChangeEvent.NetworkUnavailable -> {
                if (_callUiState.value.callState in listOf(CallState.CONNECTING, CallState.CONNECTED)) {
                    _callUiState.update { it.copy(isReconnecting = true) }
                }
            }
        }
    }

    private fun restartIceOnNetworkChange() {
        if (_callUiState.value.callId == null) return
        scope.launch {
            webRTCClient.restartIce()
            reconnectionTimeoutJob?.cancel()
            reconnectionTimeoutJob = scope.launch {
                delay(RECONNECTION_TIMEOUT_MS)
                if (_callUiState.value.isReconnecting &&
                    _callUiState.value.callState in listOf(CallState.CONNECTING, CallState.CONNECTED)
                ) {
                    _callUiState.update {
                        it.copy(callState = CallState.FAILED, errorMessage = "Connection lost - unable to reconnect", isReconnecting = false)
                    }
                    endCall()
                }
            }
        }
    }

    // --- Cleanup ---

    private fun cleanupWebRTC() {
        Log.d(TAG, "cleanupWebRTC() called")
        callObserverJob?.cancel()
        iceCandidateObserverJob?.cancel()
        durationJob?.cancel()
        reconnectionTimeoutJob?.cancel()
        iceCheckingTimeoutJob?.cancel()
        webRTCClient.close()
        pendingIceCandidates.clear()
        processedCandidateIds.clear()
        isRemoteDescriptionSet = false
        Log.d(TAG, "cleanupWebRTC() complete")
    }
}
