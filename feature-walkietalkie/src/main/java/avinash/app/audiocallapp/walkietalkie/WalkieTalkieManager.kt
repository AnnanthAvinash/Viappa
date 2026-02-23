package avinash.app.audiocallapp.walkietalkie

import android.content.Context
import android.util.Log
import avinash.app.audiocallapp.data.model.Connection
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.data.model.WtConnectionSignal
import avinash.app.audiocallapp.data.model.WtConnectionStatus
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.repository.ConnectionRepository
import avinash.app.audiocallapp.domain.repository.UserPresenceRepository
import avinash.app.audiocallapp.domain.repository.WtRole
import avinash.app.audiocallapp.domain.usecase.WalkieTalkieUseCase
import avinash.app.audiocallapp.feature.CallStateProvider
import avinash.app.audiocallapp.service.WalkieTalkieForegroundService
import avinash.app.audiocallapp.webrtc.NetworkChangeEvent
import avinash.app.audiocallapp.webrtc.NetworkChangeHandler
import avinash.app.audiocallapp.webrtc.WebRTCClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import avinash.app.audiocallapp.data.model.IceCandidate as AppIceCandidate

enum class WtPeerState {
    CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED, FAILED
}

@Singleton
class WalkieTalkieManager @Inject constructor(
    private val webRTCClient: WebRTCClient,
    private val wtUseCase: WalkieTalkieUseCase,
    private val authRepository: AuthRepository,
    private val connectionRepository: ConnectionRepository,
    private val userPresenceRepository: UserPresenceRepository,
    private val callStateProvider: CallStateProvider,
    private val networkChangeHandler: NetworkChangeHandler,
    private val context: Context
) {
    companion object {
        private const val TAG = "WT_MANAGER"
        private const val RECONNECT_BASE_DELAY_MS = 2000L
        private const val RECONNECT_MAX_DELAY_MS = 30000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Public state
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _peerStates = MutableStateFlow<Map<String, WtPeerState>>(emptyMap())
    val peerStates: StateFlow<Map<String, WtPeerState>> = _peerStates.asStateFlow()

    private val _isTalking = MutableStateFlow(false)
    val isTalking: StateFlow<Boolean> = _isTalking.asStateFlow()

    private val _talkingToFriendId = MutableStateFlow<String?>(null)
    val talkingToFriendId: StateFlow<String?> = _talkingToFriendId.asStateFlow()

    private val _remoteSpeakingFriends = MutableStateFlow<Set<String>>(emptySet())
    val remoteSpeakingFriends: StateFlow<Set<String>> = _remoteSpeakingFriends.asStateFlow()

    // Internal state
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var pool: PeerConnectionPool? = null
    private var wtAudioManager: WalkieTalkieAudioManager? = null
    private var isInitialized = false

    private var friendsObserverJob: Job? = null
    private var usersObserverJob: Job? = null
    private var callConflictJob: Job? = null
    private var networkJob: Job? = null
    private val signalingJobs = ConcurrentHashMap<String, Job>()
    private val iceCandidateJobs = ConcurrentHashMap<String, Job>()
    private val reconnectJobs = ConcurrentHashMap<String, Job>()
    private val processedIceCandidates = ConcurrentHashMap<String, MutableSet<String>>()

    private val onlineFriends = MutableStateFlow<Map<String, String>>(emptyMap()) // friendId -> friendName
    private var isCallActive = false

    fun start(userId: String, userName: String) {
        if (_isActive.value) {
            Log.d(TAG, "Already active, skipping start")
            return
        }
        Log.d(TAG, "Starting walkie-talkie for user: $userId")
        currentUserId = userId
        currentUserName = userName

        initializeWebRTC()
        _isActive.value = true

        scope.launch {
            wtUseCase.cleanupAllConnections(userId)
        }

        startAudioManager()
        observeFriendsAndPresence()
        observeCallConflicts()
        observeNetworkChanges()

        WalkieTalkieForegroundService.startService(context)
    }

    fun stop() {
        if (!_isActive.value) return
        Log.d(TAG, "Stopping walkie-talkie")
        _isActive.value = false

        stopTalking()

        friendsObserverJob?.cancel()
        usersObserverJob?.cancel()
        callConflictJob?.cancel()
        networkJob?.cancel()
        signalingJobs.values.forEach { it.cancel() }
        iceCandidateJobs.values.forEach { it.cancel() }
        reconnectJobs.values.forEach { it.cancel() }
        signalingJobs.clear()
        iceCandidateJobs.clear()
        reconnectJobs.clear()
        processedIceCandidates.clear()

        pool?.closeAll()
        wtAudioManager?.stop()
        wtAudioManager = null

        _peerStates.value = emptyMap()
        _remoteSpeakingFriends.value = emptySet()
        onlineFriends.value = emptyMap()

        scope.launch { wtUseCase.cleanupAllConnections(currentUserId) }
        WalkieTalkieForegroundService.stopService(context)
    }

    // --- Push-to-Talk ---

    fun startTalking(friendId: String) {
        if (!_isActive.value) return
        if (isCallActive) {
            Log.w(TAG, "Cannot talk on WT during active voice call")
            return
        }
        if (_peerStates.value[friendId] != WtPeerState.CONNECTED) {
            Log.w(TAG, "Cannot talk to $friendId — not connected (state: ${_peerStates.value[friendId]})")
            return
        }

        pool?.enableAudio(friendId)
        _isTalking.value = true
        _talkingToFriendId.value = friendId
        Log.d(TAG, "Started talking to $friendId")
    }

    fun stopTalking() {
        val friendId = _talkingToFriendId.value
        if (friendId != null) {
            pool?.disableAudio(friendId)
            Log.d(TAG, "Stopped talking to $friendId")
        }
        _isTalking.value = false
        _talkingToFriendId.value = null
    }

    fun isConnectedTo(friendId: String): Boolean {
        return _peerStates.value[friendId] == WtPeerState.CONNECTED
    }

    // --- WebRTC Initialization ---

    private fun initializeWebRTC() {
        if (isInitialized) return
        isInitialized = true

        webRTCClient.initialize()

        val factory = webRTCClient.getPeerConnectionFactory()
        if (factory == null) {
            Log.e(TAG, "PeerConnectionFactory is null — cannot initialize pool")
            return
        }

        pool = PeerConnectionPool(
            factory = factory,
            iceServers = WebRTCClient.getIceServersPublic()
        )
        Log.d(TAG, "WebRTC initialized, pool created")
    }

    private fun startAudioManager() {
        if (wtAudioManager == null) {
            wtAudioManager = WalkieTalkieAudioManager(context)
        }
        wtAudioManager?.start()
    }

    // --- Friends & Presence Observation ---

    private fun observeFriendsAndPresence() {
        friendsObserverJob?.cancel()
        friendsObserverJob = scope.launch {
            val friendsFlow: Flow<List<Connection>> = connectionRepository.observeFriends(currentUserId)
            val usersFlow: Flow<List<avinash.app.audiocallapp.data.model.User>> =
                userPresenceRepository.observeAvailableUsers(currentUserId)

            combine(friendsFlow, usersFlow) { friends, allUsers ->
                val userStatusMap = allUsers.associateBy({ it.uniqueId }, { it })
                val onlineMap = mutableMapOf<String, String>()
                for (friend in friends) {
                    val friendId = friend.getFriendId(currentUserId)
                    val friendName = friend.getFriendName(currentUserId)
                    val user = userStatusMap[friendId]
                    if (user != null && user.status == UserStatus.ONLINE) {
                        onlineMap[friendId] = friendName
                    }
                }
                onlineMap
            }.catch { e ->
                Log.e(TAG, "Error observing friends/presence: ${e.message}", e)
            }.collect { newOnlineFriends ->
                val previousOnline = onlineFriends.value
                onlineFriends.value = newOnlineFriends

                // Friends that came online
                val nowOnline = newOnlineFriends.keys - previousOnline.keys
                // Friends that went offline
                val nowOffline = previousOnline.keys - newOnlineFriends.keys

                for (friendId in nowOnline) {
                    val friendName = newOnlineFriends[friendId] ?: ""
                    Log.d(TAG, "Friend came online: $friendId ($friendName)")
                    connectToFriend(friendId, friendName)
                }
                for (friendId in nowOffline) {
                    Log.d(TAG, "Friend went offline: $friendId")
                    disconnectFromFriend(friendId)
                }
            }
        }
    }

    // --- Call Conflict Guard ---

    private fun observeCallConflicts() {
        callConflictJob?.cancel()
        callConflictJob = scope.launch {
            callStateProvider.isInActiveCall.collect { active ->
                val wasActive = isCallActive
                isCallActive = active

                if (isCallActive && !wasActive) {
                    Log.d(TAG, "Voice call started — pausing WT audio")
                    stopTalking()
                    pool?.disableAllAudio()
                    wtAudioManager?.stop()
                } else if (!isCallActive && wasActive) {
                    Log.d(TAG, "Voice call ended — resuming WT audio")
                    wtAudioManager?.start()
                    wtAudioManager?.ensureSpeakerOn()
                }
            }
        }
    }

    // --- Network Monitoring ---

    private fun observeNetworkChanges() {
        networkJob?.cancel()
        networkJob = scope.launch {
            networkChangeHandler.observeNetworkChanges()
                .catch { e -> Log.e(TAG, "Network observer error: ${e.message}") }
                .collect { event ->
                    when (event) {
                        is NetworkChangeEvent.NetworkTypeChanged -> {
                            Log.d(TAG, "Network changed: ${event.from} -> ${event.to}")
                            pool?.restartAllIce()
                        }
                        is NetworkChangeEvent.NetworkLost,
                        is NetworkChangeEvent.NetworkUnavailable -> {
                            Log.w(TAG, "Network lost — marking all connections as reconnecting")
                            _peerStates.update { states ->
                                states.mapValues { (_, state) ->
                                    if (state == WtPeerState.CONNECTED) WtPeerState.RECONNECTING else state
                                }
                            }
                        }
                        is NetworkChangeEvent.NetworkAvailable -> {
                            Log.d(TAG, "Network available — restarting ICE on all connections")
                            pool?.restartAllIce()
                        }
                    }
                }
        }
    }

    // --- Connect / Disconnect ---

    private fun connectToFriend(friendId: String, friendName: String) {
        if (pool?.hasConnection(friendId) == true) {
            Log.d(TAG, "Already have connection to $friendId, skipping")
            return
        }
        if (!_isActive.value) return

        val pairId = WtConnectionSignal.createPairId(currentUserId, friendId)
        val iAmOfferer = WtConnectionSignal.isOfferer(currentUserId, friendId)
        val myRole = if (iAmOfferer) WtRole.OFFERER else WtRole.ANSWERER
        val remoteRole = if (iAmOfferer) WtRole.ANSWERER else WtRole.OFFERER

        Log.d(TAG, "Connecting to $friendId (pairId=$pairId, iAmOfferer=$iAmOfferer)")
        updatePeerState(friendId, WtPeerState.CONNECTING)

        val entry = pool?.createConnection(
            friendId = friendId,
            onIceCandidate = { fId, candidate ->
                scope.launch {
                    val ice = AppIceCandidate(
                        candidate = candidate.sdp,
                        sdpMid = candidate.sdpMid ?: "",
                        sdpMLineIndex = candidate.sdpMLineIndex
                    )
                    wtUseCase.addIceCandidate(pairId, myRole, ice)
                }
            },
            onConnectionStateChange = { fId, state ->
                handlePeerConnectionStateChange(fId, friendName, state)
            },
            onRemoteTrack = { fId, track ->
                Log.d(TAG, "Remote audio track received from $fId")
            },
            onRemoteTalkStateChange = { fId, isTalking ->
                _remoteSpeakingFriends.update { current ->
                    if (isTalking) current + fId else current - fId
                }
                Log.d(TAG, "Remote talk state: $fId -> $isTalking")
            }
        )

        if (entry == null) {
            Log.e(TAG, "Failed to create peer connection for $friendId")
            updatePeerState(friendId, WtPeerState.FAILED)
            return
        }

        // Start ICE candidate observation for the remote side
        observeRemoteIceCandidates(friendId, pairId, remoteRole)

        if (iAmOfferer) {
            createAndSendOffer(friendId, friendName, pairId)
        } else {
            observeForOffer(friendId, friendName, pairId)
        }
    }

    private fun createAndSendOffer(friendId: String, friendName: String, pairId: String) {
        pool?.createOffer(friendId) { sdp ->
            scope.launch {
                val offer = SessionDescription(sdp.type.canonicalForm(), sdp.description)
                val result = wtUseCase.createOffer(
                    pairId = pairId,
                    offererId = currentUserId,
                    answererId = friendId,
                    offererName = currentUserName,
                    answererName = friendName,
                    offer = offer
                )
                if (result.isSuccess) {
                    Log.d(TAG, "Offer sent for $friendId")
                    observeForAnswer(friendId, pairId)
                } else {
                    Log.e(TAG, "Failed to send offer for $friendId: ${result.exceptionOrNull()?.message}")
                    updatePeerState(friendId, WtPeerState.FAILED)
                    scheduleReconnect(friendId, friendName)
                }
            }
        }
    }

    private fun observeForAnswer(friendId: String, pairId: String) {
        signalingJobs[friendId]?.cancel()
        signalingJobs[friendId] = scope.launch {
            wtUseCase.observeConnection(pairId)
                .filterNotNull()
                .collect { signal ->
                    if (signal.answer != null && signal.status == WtConnectionStatus.CONNECTED) {
                        Log.d(TAG, "Answer received from $friendId")
                        val remoteSdp = org.webrtc.SessionDescription(
                            org.webrtc.SessionDescription.Type.ANSWER,
                            signal.answer.sdp
                        )
                        pool?.setRemoteDescription(friendId, remoteSdp) {
                            Log.d(TAG, "Remote description set for $friendId (answer)")
                        }
                        signalingJobs[friendId]?.cancel()
                    }
                }
        }
    }

    private fun observeForOffer(friendId: String, friendName: String, pairId: String) {
        signalingJobs[friendId]?.cancel()
        signalingJobs[friendId] = scope.launch {
            wtUseCase.observeConnection(pairId)
                .filterNotNull()
                .filter { it.offer != null && it.status == WtConnectionStatus.OFFERING }
                .collect { signal ->
                    Log.d(TAG, "Offer received from $friendId")
                    val remoteSdp = org.webrtc.SessionDescription(
                        org.webrtc.SessionDescription.Type.OFFER,
                        signal.offer!!.sdp
                    )
                    pool?.setRemoteDescription(friendId, remoteSdp) {
                        pool?.createAnswer(friendId) { answerSdp ->
                            scope.launch {
                                val answer = SessionDescription(
                                    answerSdp.type.canonicalForm(),
                                    answerSdp.description
                                )
                                wtUseCase.updateAnswer(pairId, answer)
                                Log.d(TAG, "Answer sent for $friendId")
                            }
                        }
                    }
                    signalingJobs[friendId]?.cancel()
                }
        }
    }

    private fun observeRemoteIceCandidates(friendId: String, pairId: String, remoteRole: WtRole) {
        processedIceCandidates[friendId] = mutableSetOf()
        iceCandidateJobs[friendId]?.cancel()
        iceCandidateJobs[friendId] = scope.launch {
            wtUseCase.observeIceCandidates(pairId, remoteRole)
                .collect { candidates ->
                    val processed = processedIceCandidates[friendId] ?: mutableSetOf()
                    for (candidate in candidates) {
                        val key = "${candidate.sdpMid}:${candidate.sdpMLineIndex}:${candidate.candidate.hashCode()}"
                        if (key !in processed) {
                            processed.add(key)
                            val iceCandidate = org.webrtc.IceCandidate(
                                candidate.sdpMid,
                                candidate.sdpMLineIndex,
                                candidate.candidate
                            )
                            pool?.addIceCandidate(friendId, iceCandidate)
                        }
                    }
                }
        }
    }

    private fun disconnectFromFriend(friendId: String) {
        Log.d(TAG, "Disconnecting from $friendId")
        signalingJobs.remove(friendId)?.cancel()
        iceCandidateJobs.remove(friendId)?.cancel()
        reconnectJobs.remove(friendId)?.cancel()
        processedIceCandidates.remove(friendId)

        if (_talkingToFriendId.value == friendId) {
            stopTalking()
        }

        pool?.closeConnection(friendId)
        updatePeerState(friendId, WtPeerState.DISCONNECTED)

        _remoteSpeakingFriends.update { it - friendId }

        val pairId = WtConnectionSignal.createPairId(currentUserId, friendId)
        scope.launch {
            try { wtUseCase.cleanupConnection(pairId) } catch (_: Exception) {}
        }

        // Remove from peer states after a short delay so UI can show disconnect state
        scope.launch {
            delay(2000)
            _peerStates.update { it - friendId }
        }
    }

    // --- Connection State Handling ---

    private fun handlePeerConnectionStateChange(
        friendId: String,
        friendName: String,
        state: PeerConnection.PeerConnectionState
    ) {
        Log.d(TAG, "[$friendId] Peer connection state: $state")
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTING -> {
                updatePeerState(friendId, WtPeerState.CONNECTING)
            }
            PeerConnection.PeerConnectionState.CONNECTED -> {
                reconnectJobs.remove(friendId)?.cancel()
                updatePeerState(friendId, WtPeerState.CONNECTED)
                wtAudioManager?.ensureSpeakerOn()
                Log.d(TAG, "[$friendId] WT connection ESTABLISHED")
            }
            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                updatePeerState(friendId, WtPeerState.RECONNECTING)
                pool?.restartIce(friendId)
                scheduleReconnect(friendId, friendName)
            }
            PeerConnection.PeerConnectionState.FAILED -> {
                updatePeerState(friendId, WtPeerState.FAILED)
                if (_talkingToFriendId.value == friendId) stopTalking()
                scheduleReconnect(friendId, friendName)
            }
            PeerConnection.PeerConnectionState.CLOSED -> {
                updatePeerState(friendId, WtPeerState.DISCONNECTED)
            }
            else -> {}
        }
    }

    private fun scheduleReconnect(friendId: String, friendName: String) {
        reconnectJobs.remove(friendId)?.cancel()
        reconnectJobs[friendId] = scope.launch {
            var attempt = 0
            while (_isActive.value && onlineFriends.value.containsKey(friendId)) {
                val delay = minOf(
                    RECONNECT_BASE_DELAY_MS * (1 shl attempt),
                    RECONNECT_MAX_DELAY_MS
                )
                Log.d(TAG, "[$friendId] Reconnect attempt ${attempt + 1} in ${delay}ms")
                delay(delay)

                if (!_isActive.value || !onlineFriends.value.containsKey(friendId)) break

                pool?.closeConnection(friendId)
                signalingJobs.remove(friendId)?.cancel()
                iceCandidateJobs.remove(friendId)?.cancel()
                processedIceCandidates.remove(friendId)

                val pairId = WtConnectionSignal.createPairId(currentUserId, friendId)
                try { wtUseCase.cleanupConnection(pairId) } catch (_: Exception) {}

                connectToFriend(friendId, friendName)

                // Wait for connection to establish or fail
                delay(15000)
                if (_peerStates.value[friendId] == WtPeerState.CONNECTED) {
                    Log.d(TAG, "[$friendId] Reconnect successful")
                    break
                }
                attempt++
                if (attempt >= 5) {
                    Log.e(TAG, "[$friendId] Max reconnect attempts reached")
                    updatePeerState(friendId, WtPeerState.FAILED)
                    break
                }
            }
        }
    }

    private fun updatePeerState(friendId: String, state: WtPeerState) {
        _peerStates.update { it + (friendId to state) }
    }
}
