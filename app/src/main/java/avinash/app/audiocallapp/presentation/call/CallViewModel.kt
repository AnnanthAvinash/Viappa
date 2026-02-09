package avinash.app.audiocallapp.presentation.call

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import avinash.app.audiocallapp.data.model.CallStatus
import avinash.app.audiocallapp.data.model.IceCandidate
import android.content.Context
import avinash.app.audiocallapp.data.model.SessionDescription
import dagger.hilt.android.qualifiers.ApplicationContext
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.usecase.CallUseCase
import avinash.app.audiocallapp.service.CallForegroundService
import avinash.app.audiocallapp.webrtc.NetworkChangeEvent
import avinash.app.audiocallapp.webrtc.NetworkChangeHandler
import avinash.app.audiocallapp.webrtc.NetworkType
import avinash.app.audiocallapp.webrtc.WebRTCClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import javax.inject.Inject

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

@HiltViewModel
class CallViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val callUseCase: CallUseCase,
    private val authRepository: AuthRepository,
    private val webRTCClient: WebRTCClient,
    private val networkChangeHandler: NetworkChangeHandler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "AudioCallApp"
        private const val KEY_CALL_ID = "call_id"
        private const val KEY_REMOTE_USER_ID = "remote_user_id"
        private const val KEY_REMOTE_USER_NAME = "remote_user_name"
        private const val KEY_IS_CALLER = "is_caller"
        private const val KEY_CALL_STATE = "call_state"
        private const val RECONNECTION_TIMEOUT_MS = 30_000L
        private const val ICE_CHECKING_TIMEOUT_MS = 30_000L
    }

    private val _uiState = MutableStateFlow(
        CallUiState(
            callState = CallState.valueOf(
                savedStateHandle.get<String>(KEY_CALL_STATE) ?: CallState.IDLE.name
            ),
            callId = savedStateHandle.get<String>(KEY_CALL_ID),
            remoteUserId = savedStateHandle.get<String>(KEY_REMOTE_USER_ID) ?: "",
            remoteUserName = savedStateHandle.get<String>(KEY_REMOTE_USER_NAME) ?: "",
            isCaller = savedStateHandle.get<Boolean>(KEY_IS_CALLER) ?: true
        )
    )
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var callObserverJob: Job? = null
    private var iceCandidateObserverJob: Job? = null
    private var durationJob: Job? = null
    private var networkObserverJob: Job? = null
    private var reconnectionTimeoutJob: Job? = null

    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private val processedCandidateIds = mutableSetOf<String>()
    private var isRemoteDescriptionSet = false
    private var previousNetworkType: NetworkType = NetworkType.UNKNOWN
    private var iceCheckingTimeoutJob: Job? = null

    init {
        webRTCClient.initialize()
        
        // Restore call state if process was killed
        restoreCallState()
        
        // Start network monitoring
        startNetworkMonitoring()
    }

    fun initiateOutgoingCall(calleeId: String, calleeName: String) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: run {
                Log.e(TAG, "[OUTGOING] User not authenticated")
                _uiState.update {
                    it.copy(
                        callState = CallState.FAILED,
                        errorMessage = "User not authenticated"
                    )
                }
                return@launch
            }


            Log.d(TAG, "[OUTGOING] Starting call to $calleeName ($calleeId)")

            _uiState.update {
                it.copy(
                    callState = CallState.OUTGOING,
                    remoteUserId = calleeId,
                    remoteUserName = calleeName,
                    isCaller = true,
                    networkType = networkChangeHandler.getCurrentNetworkType()
                )
            }
            
            saveCallState()

            try {
                Log.d(TAG, "[OUTGOING] Creating peer connection...")
                webRTCClient.createPeerConnection(
                    onIceCandidate = { candidate ->
                        viewModelScope.launch {
                            val callId = _uiState.value.callId
                            if (callId != null) {
                                Log.d(TAG, "ICE candidate received, adding to Firestore")
                                callUseCase.addCallerIceCandidate(
                                    callId,
                                    IceCandidate(
                                        candidate = candidate.sdp,
                                        sdpMid = candidate.sdpMid ?: "",
                                        sdpMLineIndex = candidate.sdpMLineIndex
                                    )
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
                Log.d(TAG, "Peer connection created successfully")

                // Create offer with timeout handling
                var offerCreated = false
                Log.d(TAG, "About to call webRTCClient.createOffer()...")
                Log.d(TAG, "Creating WebRTC offer...")
                webRTCClient.createOffer { sdp ->
                    Log.d(TAG, "createOffer callback invoked! sdp type: ${sdp.type}")
                    offerCreated = true
                    Log.d(TAG, "Offer created successfully, type: ${sdp.type}, sdp length: ${sdp.description.length}")
                    viewModelScope.launch {
                        try {
                            val offer = SessionDescription(
                                type = sdp.type.canonicalForm(),
                                sdp = sdp.description
                            )

                            Log.d(TAG, "Creating call in Firestore with offer...")
                            val result = callUseCase.initiateCall(
                                callerId = currentUser.uniqueId,
                                calleeId = calleeId,
                                callerName = currentUser.displayName,
                                calleeName = calleeName,
                                offer = offer
                            )

                            result.fold(
                                onSuccess = { callId ->
                                    Log.d(TAG, "Call created successfully in Firestore, callId: $callId")
                                    _uiState.update { it.copy(callId = callId) }
                                    saveCallState()
                                    
                                    // Start foreground service
                                    try {
                                        CallForegroundService.startService(
                                            context,
                                            calleeName,
                                            isIncoming = false,
                                            callId = callId
                                        )
                                        Log.d(TAG, "Foreground service started")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to start foreground service", e)
                                        // Don't fail the call if service fails
                                    }
                                    
                                    Log.d(TAG, "Starting to observe call and ICE candidates...")
                                    observeCall(callId)
                                    observeCalleeIceCandidates(callId)
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Failed to create call in Firestore", error)
                                    _uiState.update {
                                        it.copy(
                                            callState = CallState.FAILED,
                                            errorMessage = error.message ?: "Failed to create call"
                                        )
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing offer", e)
                            _uiState.update {
                                it.copy(
                                    callState = CallState.FAILED,
                                    errorMessage = "Error creating offer: ${e.message}"
                                )
                            }
                        }
                    }
                }
                
                // Add timeout check for offer creation
                viewModelScope.launch {
                    delay(10000) // 10 second timeout
                    if (!offerCreated) {
                        Log.e(TAG, "Offer creation timed out after 10 seconds")
                        _uiState.update {
                            it.copy(
                                callState = CallState.FAILED,
                                errorMessage = "Call setup timed out. Please check your connection and try again."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating peer connection", e)
                _uiState.update {
                    it.copy(
                        callState = CallState.FAILED,
                        errorMessage = "Error setting up call: ${e.message}"
                    )
                }
            }
        }
    }

    fun answerIncomingCall(callId: String, callerId: String, callerName: String) {
        viewModelScope.launch {
            Log.d(TAG, "[INCOMING] Answering call from $callerName ($callerId)")
            
            _uiState.update {
                it.copy(
                    callState = CallState.INCOMING,
                    callId = callId,
                    remoteUserId = callerId,
                    remoteUserName = callerName,
                    isCaller = false,
                    networkType = networkChangeHandler.getCurrentNetworkType()
                )
            }
            
            saveCallState()

            Log.d(TAG, "[INCOMING] Creating peer connection...")
            webRTCClient.createPeerConnection(
                onIceCandidate = { candidate ->
                    viewModelScope.launch {
                        callUseCase.addCalleeIceCandidate(
                            callId,
                            IceCandidate(
                                candidate = candidate.sdp,
                                sdpMid = candidate.sdpMid ?: "",
                                sdpMLineIndex = candidate.sdpMLineIndex
                            )
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
            Log.d(TAG, "[INCOMING] Peer connection created")

            CallForegroundService.startService(
                context,
                callerName,
                isIncoming = true,
                callId = callId
            )
            
            Log.d(TAG, "[INCOMING] Waiting for offer...")
            observeCall(callId)
            observeCallerIceCandidates(callId)
        }
    }

    private fun observeCall(callId: String) {
        callObserverJob?.cancel()
        callObserverJob = viewModelScope.launch {
            val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
            Log.d(TAG, "$prefix Observing call: $callId")
            callUseCase.observeCall(callId)
                .catch { e ->
                    Log.e(TAG, "$prefix Error: ${e.message}")
                }
                .collect { call ->
                    if (call == null) {
                        Log.w(TAG, "$prefix Call ended")
                        _uiState.update { it.copy(callState = CallState.ENDED) }
                        return@collect
                    }

                    when (call.status) {
                        CallStatus.RINGING -> {
                            if (!_uiState.value.isCaller && call.offer != null && !isRemoteDescriptionSet) {
                                Log.d(TAG, "[INCOMING] Processing offer (${call.offer.sdp.length} bytes)...")
                                val remoteSdp = org.webrtc.SessionDescription(
                                    org.webrtc.SessionDescription.Type.OFFER,
                                    call.offer.sdp
                                )
                                webRTCClient.setRemoteDescription(remoteSdp) {
                                    Log.d(TAG, "[INCOMING] Offer set, creating answer...")
                                    isRemoteDescriptionSet = true
                                    processPendingIceCandidates()

                                    webRTCClient.createAnswer { answerSdp ->
                                        Log.d(TAG, "[INCOMING] Answer created (${answerSdp.description.length} bytes)")
                                        viewModelScope.launch {
                                            val answer = SessionDescription(
                                                type = answerSdp.type.canonicalForm(),
                                                sdp = answerSdp.description
                                            )
                                            Log.d(TAG, "[INCOMING] Sending answer...")
                                            callUseCase.answerCall(callId, answer)
                                            Log.d(TAG, "[INCOMING] Setting UI state to CONNECTING")
                                            _uiState.update { it.copy(callState = CallState.CONNECTING) }
                                        }
                                    }
                                }
                            }
                        }
                        CallStatus.ACCEPTED -> {
                            if (_uiState.value.isCaller && call.answer != null && !isRemoteDescriptionSet) {
                                Log.d(TAG, "[OUTGOING] Answer received (${call.answer.sdp.length} bytes)")
                                val remoteSdp = org.webrtc.SessionDescription(
                                    org.webrtc.SessionDescription.Type.ANSWER,
                                    call.answer.sdp
                                )
                                webRTCClient.setRemoteDescription(remoteSdp) {
                                    Log.d(TAG, "[OUTGOING] Answer set")
                                    isRemoteDescriptionSet = true
                                    processPendingIceCandidates()
                                }
                            }
                            Log.d(TAG, "[OUTGOING] Setting UI state to CONNECTING")
                            _uiState.update { it.copy(callState = CallState.CONNECTING) }
                        }
                        CallStatus.REJECTED -> {
                            Log.d(TAG, "$prefix Call rejected")
                            _uiState.update {
                                it.copy(
                                    callState = CallState.ENDED,
                                    errorMessage = "Call was rejected"
                                )
                            }
                        }
                        CallStatus.ENDED -> {
                            Log.d(TAG, "$prefix Call ended")
                            _uiState.update { it.copy(callState = CallState.ENDED) }
                        }
                    }
                }
            }
    }

    private fun observeCallerIceCandidates(callId: String) {
        iceCandidateObserverJob?.cancel()
        iceCandidateObserverJob = viewModelScope.launch {
            val prefix = "[INCOMING]"  // Callee receives caller's candidates
            Log.d(TAG, "$prefix [ICE] Listening for caller candidates")
            callUseCase.observeCallerIceCandidates(callId).collect { candidates ->
                val processed = processedCandidateIds.size
                val total = candidates.size
                val newCount = maxOf(0, total - processed)
                if (candidates.isNotEmpty()) {
                    Log.d(TAG, "$prefix [ICE] Snapshot: $total total, ~$newCount new")
                }
                candidates.forEach { addIceCandidate(it) }
            }
        }
    }

    private fun observeCalleeIceCandidates(callId: String) {
        iceCandidateObserverJob?.cancel()
        iceCandidateObserverJob = viewModelScope.launch {
            val prefix = "[OUTGOING]"  // Caller receives callee's candidates
            Log.d(TAG, "$prefix [ICE] Listening for callee candidates")
            callUseCase.observeCalleeIceCandidates(callId).collect { candidates ->
                val processed = processedCandidateIds.size
                val total = candidates.size
                val newCount = maxOf(0, total - processed)
                if (candidates.isNotEmpty()) {
                    Log.d(TAG, "$prefix [ICE] Snapshot: $total total, ~$newCount new")
                }
                candidates.forEach { addIceCandidate(it) }
            }
        }
    }

    private fun addIceCandidate(candidate: IceCandidate) {
        val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        val candidateKey = "${candidate.sdpMid}:${candidate.sdpMLineIndex}"
        val candidateId = "$candidateKey:${candidate.candidate.hashCode()}"
        
        // Skip duplicate candidates
        if (candidateId in processedCandidateIds) {
            Log.d(TAG, "$prefix [ICE] Skipped duplicate $candidateKey")
            return
        }
        processedCandidateIds.add(candidateId)
        
        if (isRemoteDescriptionSet) {
            val iceCandidate = org.webrtc.IceCandidate(
                candidate.sdpMid,
                candidate.sdpMLineIndex,
                candidate.candidate
            )
            Log.d(TAG, "$prefix [ICE] Adding $candidateKey")
            val success = webRTCClient.addIceCandidate(iceCandidate)
            Log.d(TAG, "$prefix [ICE] Added $candidateKey -> ${if (success) "OK" else "FAILED"}")
        } else {
            pendingIceCandidates.add(candidate)
            Log.d(TAG, "$prefix [ICE] Queued $candidateKey (${pendingIceCandidates.size} pending)")
        }
    }

    private fun processPendingIceCandidates() {
        val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        if (pendingIceCandidates.isNotEmpty()) {
            Log.d(TAG, "$prefix Processing ${pendingIceCandidates.size} pending ICE candidates")
        }
        pendingIceCandidates.forEach { candidate ->
            val iceCandidate = org.webrtc.IceCandidate(
                candidate.sdpMid,
                candidate.sdpMLineIndex,
                candidate.candidate
            )
            webRTCClient.addIceCandidate(iceCandidate)
        }
        pendingIceCandidates.clear()
    }

    private fun handleConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        Log.d(TAG, "$prefix [ICE] Connection state: $state")
        when (state) {
            PeerConnection.PeerConnectionState.NEW -> {
                Log.d(TAG, "$prefix [ICE] State: NEW")
            }
            PeerConnection.PeerConnectionState.CONNECTING -> {
                Log.d(TAG, "$prefix [ICE] State: CONNECTING - starting 30s timeout")
                startIceCheckingTimeout()
                _uiState.update { it.copy(callState = CallState.CONNECTING) }
            }
            PeerConnection.PeerConnectionState.CONNECTED -> {
                Log.d(TAG, "$prefix [ICE] State: CONNECTED - call active!")
                iceCheckingTimeoutJob?.cancel()
                reconnectionTimeoutJob?.cancel()
                _uiState.update { 
                    it.copy(
                        callState = CallState.CONNECTED,
                        isReconnecting = false
                    ) 
                }
                startCallDurationTimer()
                saveCallState()
            }
            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                Log.w(TAG, "$prefix [ICE] State: DISCONNECTED")
                _uiState.update {
                    it.copy(
                        callState = CallState.FAILED,
                        errorMessage = "Connection lost"
                    )
                }
            }
            PeerConnection.PeerConnectionState.FAILED -> {
                Log.e(TAG, "$prefix [ICE] State: FAILED")
                iceCheckingTimeoutJob?.cancel()
                _uiState.update {
                    it.copy(
                        callState = CallState.FAILED,
                        errorMessage = "Connection failed. TURN server may be required."
                    )
                }
            }
            PeerConnection.PeerConnectionState.CLOSED -> {
                Log.d(TAG, "$prefix [ICE] State: CLOSED")
                iceCheckingTimeoutJob?.cancel()
                _uiState.update { it.copy(callState = CallState.ENDED) }
            }
            else -> {
                Log.d(TAG, "$prefix [ICE] State: $state")
            }
        }
    }
    
    private fun startIceCheckingTimeout() {
        val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        iceCheckingTimeoutJob?.cancel()
        iceCheckingTimeoutJob = viewModelScope.launch {
            delay(ICE_CHECKING_TIMEOUT_MS)
            if (_uiState.value.callState == CallState.CONNECTING) {
                Log.e(TAG, "$prefix [ICE] TIMEOUT after 30s - need TURN server?")
                _uiState.update {
                    it.copy(
                        callState = CallState.FAILED,
                        errorMessage = "Connection timeout. TURN server may be required."
                    )
                }
                endCall()
            }
        }
    }

    private fun startCallDurationTimer() {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            var duration = 0L
            while (true) {
                _uiState.update { it.copy(callDuration = duration) }
                kotlinx.coroutines.delay(1000)
                duration++
            }
        }
    }

    fun toggleMute() {
        val newMuteState = !_uiState.value.isMuted
        val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        Log.d(TAG, "$prefix Mute: $newMuteState")
        webRTCClient.setMicrophoneMuted(newMuteState)
        _uiState.update { it.copy(isMuted = newMuteState) }
    }

    fun toggleSpeaker() {
        val newSpeakerState = !_uiState.value.isSpeakerOn
        val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        Log.d(TAG, "$prefix Speaker: $newSpeakerState")
        webRTCClient.setSpeakerEnabled(newSpeakerState)
        _uiState.update { it.copy(isSpeakerOn = newSpeakerState) }
    }

    fun endCall() {
        val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        Log.d(TAG, "$prefix Ending call...")
        viewModelScope.launch {
            val callId = _uiState.value.callId
            if (callId != null) {
                Log.d(TAG, "$prefix Updating call status to ENDED")
                callUseCase.endCall(callId)
                kotlinx.coroutines.delay(500)
                Log.d(TAG, "$prefix Cleaning up call")
                callUseCase.cleanupCall(callId)
            }
            Log.d(TAG, "$prefix Stopping service")
            CallForegroundService.stopService(context)
            cleanup()
            _uiState.update { it.copy(callState = CallState.ENDED) }
            Log.d(TAG, "$prefix Call ended")
        }
    }

    private fun startNetworkMonitoring() {
        networkObserverJob?.cancel()
        networkObserverJob = viewModelScope.launch {
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
        val prefix = if (_uiState.value.isCaller) "[OUTGOING]" else "[INCOMING]"
        when (event) {
            is NetworkChangeEvent.NetworkAvailable -> {
                Log.d(TAG, "$prefix Network: ${event.networkType}")
                _uiState.update { it.copy(networkType = event.networkType) }
                previousNetworkType = event.networkType
            }
            is NetworkChangeEvent.NetworkTypeChanged -> {
                Log.d(TAG, "$prefix Network changed: ${event.from} -> ${event.to}")
                _uiState.update { 
                    it.copy(
                        networkType = event.to,
                        isReconnecting = true
                    ) 
                }
                previousNetworkType = event.to
                
                if (_uiState.value.callState in listOf(CallState.CONNECTING, CallState.CONNECTED)) {
                    Log.d(TAG, "$prefix Restarting ICE...")
                    restartIceOnNetworkChange()
                }
            }
            is NetworkChangeEvent.NetworkLost -> {
                Log.d(TAG, "$prefix Network lost")
                if (_uiState.value.callState in listOf(CallState.CONNECTING, CallState.CONNECTED)) {
                    _uiState.update { it.copy(isReconnecting = true) }
                }
            }
            is NetworkChangeEvent.NetworkUnavailable -> {
                Log.d(TAG, "$prefix Network unavailable")
                if (_uiState.value.callState in listOf(CallState.CONNECTING, CallState.CONNECTED)) {
                    _uiState.update { it.copy(isReconnecting = true) }
                }
            }
        }
    }

    private fun restartIceOnNetworkChange() {
        val currentState = _uiState.value
        if (currentState.callId == null) return

        val prefix = if (currentState.isCaller) "[OUTGOING]" else "[INCOMING]"
        Log.d(TAG, "$prefix Restarting ICE...")
        
        viewModelScope.launch {
            webRTCClient.restartIce()
            Log.d(TAG, "$prefix ICE restarted")
            
            reconnectionTimeoutJob?.cancel()
            reconnectionTimeoutJob = viewModelScope.launch {
                kotlinx.coroutines.delay(RECONNECTION_TIMEOUT_MS)
                
                if (_uiState.value.isReconnecting && 
                    _uiState.value.callState in listOf(CallState.CONNECTING, CallState.CONNECTED)) {
                    Log.e(TAG, "$prefix Reconnection timeout")
                    _uiState.update {
                        it.copy(
                            callState = CallState.FAILED,
                            errorMessage = "Connection lost - unable to reconnect",
                            isReconnecting = false
                        )
                    }
                    endCall()
                }
            }
        }
    }

    private fun saveCallState() {
        val state = _uiState.value
        val prefix = if (state.isCaller) "[OUTGOING]" else "[INCOMING]"
        savedStateHandle[KEY_CALL_ID] = state.callId
        savedStateHandle[KEY_REMOTE_USER_ID] = state.remoteUserId
        savedStateHandle[KEY_REMOTE_USER_NAME] = state.remoteUserName
        savedStateHandle[KEY_IS_CALLER] = state.isCaller
        savedStateHandle[KEY_CALL_STATE] = state.callState.name
        Log.d(TAG, "$prefix State saved: ${state.callState}")
    }

    private fun restoreCallState() {
        val callId = savedStateHandle.get<String>(KEY_CALL_ID)
        val remoteUserId = savedStateHandle.get<String>(KEY_REMOTE_USER_ID)
        val remoteUserName = savedStateHandle.get<String>(KEY_REMOTE_USER_NAME)
        val isCaller = savedStateHandle.get<Boolean>(KEY_IS_CALLER) ?: true
        val callState = CallState.valueOf(
            savedStateHandle.get<String>(KEY_CALL_STATE) ?: CallState.IDLE.name
        )

        val prefix = if (isCaller) "[OUTGOING]" else "[INCOMING]"
        
        if (callId != null && remoteUserId != null && 
            callState in listOf(CallState.OUTGOING, CallState.INCOMING, CallState.CONNECTING, CallState.CONNECTED)) {
            Log.d(TAG, "$prefix Restoring call: $callId, state=$callState")
            
            viewModelScope.launch {
                Log.d(TAG, "$prefix Recreating peer connection...")
                webRTCClient.createPeerConnection(
                    onIceCandidate = { candidate ->
                        viewModelScope.launch {
                            if (isCaller) {
                                callUseCase.addCallerIceCandidate(
                                    callId,
                                    IceCandidate(
                                        candidate = candidate.sdp,
                                        sdpMid = candidate.sdpMid ?: "",
                                        sdpMLineIndex = candidate.sdpMLineIndex
                                    )
                                )
                            } else {
                                callUseCase.addCalleeIceCandidate(
                                    callId,
                                    IceCandidate(
                                        candidate = candidate.sdp,
                                        sdpMid = candidate.sdpMid ?: "",
                                        sdpMLineIndex = candidate.sdpMLineIndex
                                    )
                                )
                            }
                        }
                    },
                    onConnectionStateChange = { state ->
                        handleConnectionStateChange(state)
                    },
                    onRemoteTrack = { track ->
                        Log.d(TAG, "Remote audio track received")
                    }
                )

                Log.d(TAG, "$prefix Re-observing call and ICE candidates...")
                observeCall(callId)
                if (isCaller) {
                    observeCalleeIceCandidates(callId)
                } else {
                    observeCallerIceCandidates(callId)
                }
                Log.d(TAG, "$prefix State restored")
            }
        }
    }

    private fun cleanup() {
        Log.d(TAG, "Cleaning up...")
        callObserverJob?.cancel()
        iceCandidateObserverJob?.cancel()
        durationJob?.cancel()
        networkObserverJob?.cancel()
        reconnectionTimeoutJob?.cancel()
        iceCheckingTimeoutJob?.cancel()
        webRTCClient.close()
        pendingIceCandidates.clear()
        processedCandidateIds.clear()
        isRemoteDescriptionSet = false
        
        savedStateHandle.remove<String>(KEY_CALL_ID)
        savedStateHandle.remove<String>(KEY_REMOTE_USER_ID)
        savedStateHandle.remove<String>(KEY_REMOTE_USER_NAME)
        savedStateHandle.remove<Boolean>(KEY_IS_CALLER)
        savedStateHandle.remove<String>(KEY_CALL_STATE)
        Log.d(TAG, "Cleanup complete")
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
        networkChangeHandler.stop()
        webRTCClient.dispose()
    }
}