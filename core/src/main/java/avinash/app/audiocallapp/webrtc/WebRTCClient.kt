package avinash.app.audiocallapp.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCClient @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AudioCallApp"
        
        // STUN servers (attempted first for direct P2P)
        private val STUN_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
        )
        
        // TURN servers (fallback when STUN fails - symmetric NAT, CGNAT, firewalls)
        // Configure these by calling setTurnServers() or set via environment variables
        // Example: Metered.ca free tier or Twilio TURN
        // Format: "turn:server:port" with username and password
        @Volatile
        private var customTurnServers: List<PeerConnection.IceServer> = emptyList()
        
        /**
         * Configure TURN servers for production use.
         * Call this method with your TURN server credentials.
         * 
         * @param server TURN server address (e.g., "a.relay.metered.ca" or "your-turn-server.com")
         * @param username TURN server username
         * @param password TURN server password
         */
        fun setTurnServers(server: String, username: String, password: String) {
            if (server.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                customTurnServers = listOf(
                    // UDP TURN
                    PeerConnection.IceServer.builder("turn:$server:80")
                        .setUsername(username)
                        .setPassword(password)
                        .createIceServer(),
                    // TCP TURN
                    PeerConnection.IceServer.builder("turn:$server:443?transport=tcp")
                        .setUsername(username)
                        .setPassword(password)
                        .createIceServer()
                )
                Log.d(TAG, "[TURN] Configured: $server")
            } else {
                customTurnServers = emptyList()
                Log.d(TAG, "[TURN] Disabled - no credentials")
            }
        }
        
        // Combined ICE servers - WebRTC will automatically prioritize STUN over TURN
        private fun getIceServers(): List<PeerConnection.IceServer> {
            return STUN_SERVERS + customTurnServers
        }

        fun getIceServersPublic(): List<PeerConnection.IceServer> = getIceServers()
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioManager: RTCAudioManager? = null

    private var onIceCandidateListener: ((IceCandidate) -> Unit)? = null
    private var onConnectionStateChangeListener: ((PeerConnection.PeerConnectionState) -> Unit)? = null
    private var onRemoteTrackListener: ((MediaStreamTrack) -> Unit)? = null
    private var onIceGatheringStateChangeListener: ((PeerConnection.IceGatheringState) -> Unit)? = null
    
    private var iceGatheringState: PeerConnection.IceGatheringState = PeerConnection.IceGatheringState.NEW

    fun initialize() {
        Log.d(TAG, "Initializing WebRTC")
        
        try {
            val options = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            Log.d(TAG, "Initializing PeerConnectionFactory...")
            PeerConnectionFactory.initialize(options)
            Log.d(TAG, "PeerConnectionFactory initialized")

            Log.d(TAG, "Creating EGL base...")
            val eglBase = EglBase.create()
            Log.d(TAG, "Creating encoder/decoder factories...")
            val encoderFactory = DefaultVideoEncoderFactory(
                eglBase.eglBaseContext,
                true,
                true
            )
            val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

            Log.d(TAG, "Creating PeerConnectionFactory...")
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
            Log.d(TAG, "PeerConnectionFactory created successfully: ${peerConnectionFactory != null}")

            Log.d(TAG, "Initializing audio manager...")
            audioManager = RTCAudioManager(context)
            audioManager?.start()
            Log.d(TAG, "WebRTC initialization complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WebRTC", e)
            throw e
        }
    }

    fun createPeerConnection(
        onIceCandidate: (IceCandidate) -> Unit,
        onConnectionStateChange: (PeerConnection.PeerConnectionState) -> Unit,
        onRemoteTrack: (MediaStreamTrack) -> Unit
    ) {
        Log.d(TAG, "[WEBRTC] Creating peer connection...")
        Log.d(TAG, "[WEBRTC] Factory: ${if (peerConnectionFactory == null) "NULL" else "OK"}")
        
        if (peerConnectionFactory == null) {
            Log.e(TAG, "[WEBRTC] Cannot create peer connection: factory is null!")
            return
        }
        
        this.onIceCandidateListener = onIceCandidate
        this.onConnectionStateChangeListener = onConnectionStateChange
        this.onRemoteTrackListener = onRemoteTrack

        val iceServers = getIceServers()
        Log.d(TAG, "[WEBRTC] ICE servers: ${iceServers.size}")
        iceServers.forEachIndexed { index, server ->
            Log.d(TAG, "[WEBRTC] ICE server $index: ${server.urls.joinToString()}")
        }
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        Log.d(TAG, "[WEBRTC] Creating peer connection with config...")
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    Log.d(TAG, "[WEBRTC] Signaling state: $state")
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "[WEBRTC] ICE connection: $state")
                    when (state) {
                        PeerConnection.IceConnectionState.NEW -> {
                            Log.d(TAG, "[WEBRTC] ICE: NEW - Starting connection")
                        }
                        PeerConnection.IceConnectionState.CHECKING -> {
                            Log.d(TAG, "[WEBRTC] ICE: CHECKING - Checking connectivity")
                        }
                        PeerConnection.IceConnectionState.CONNECTED -> {
                            Log.d(TAG, "[WEBRTC] ICE: CONNECTED - Connection established")
                        }
                        PeerConnection.IceConnectionState.COMPLETED -> {
                            Log.d(TAG, "[WEBRTC] ICE: COMPLETED - All candidates processed")
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            Log.e(TAG, "[WEBRTC] ICE: FAILED - Connection failed")
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            Log.w(TAG, "[WEBRTC] ICE: DISCONNECTED - Connection lost")
                        }
                        PeerConnection.IceConnectionState.CLOSED -> {
                            Log.d(TAG, "[WEBRTC] ICE: CLOSED - Connection closed")
                        }
                        else -> {}
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Log.d(TAG, "[WEBRTC] ICE receiving: $receiving")
                }

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    Log.d(TAG, "[WEBRTC] ICE gathering: $state")
                    state?.let {
                        iceGatheringState = it
                        when (it) {
                            PeerConnection.IceGatheringState.NEW -> {
                                Log.d(TAG, "[WEBRTC] ICE gathering: NEW")
                            }
                            PeerConnection.IceGatheringState.GATHERING -> {
                                Log.d(TAG, "[WEBRTC] ICE gathering: GATHERING - Collecting candidates")
                            }
                            PeerConnection.IceGatheringState.COMPLETE -> {
                                Log.d(TAG, "[WEBRTC] ICE gathering: COMPLETE - All candidates collected")
                            }
                            else -> {}
                        }
                        onIceGatheringStateChangeListener?.invoke(it)
                    }
                }

                override fun onIceCandidate(candidate: IceCandidate?) {
                    if (candidate != null) {
                        Log.d(TAG, "[WEBRTC] ICE candidate: ${candidate.sdpMid}:${candidate.sdpMLineIndex} - ${candidate.sdp.substring(0, minOf(50, candidate.sdp.length))}...")
                    } else {
                        Log.w(TAG, "[WEBRTC] ICE candidate: NULL")
                    }
                    candidate?.let { onIceCandidateListener?.invoke(it) }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                    Log.d(TAG, "onIceCandidatesRemoved")
                }

                override fun onAddStream(stream: MediaStream?) {
                    Log.d(TAG, "onAddStream")
                }

                override fun onRemoveStream(stream: MediaStream?) {
                    Log.d(TAG, "onRemoveStream")
                }

                override fun onDataChannel(channel: DataChannel?) {
                    Log.d(TAG, "onDataChannel")
                }

                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded")
                }

                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    Log.d(TAG, "[WEBRTC] onAddTrack: ${receiver?.track()?.kind()}")
                    receiver?.track()?.let { track ->
                        if (track.kind() == MediaStreamTrack.AUDIO_TRACK_KIND) {
                            Log.d(TAG, "[WEBRTC] Remote audio track added")
                            onRemoteTrackListener?.invoke(track)
                        }
                    }
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val trackKind = transceiver?.receiver?.track()?.kind()
                    Log.d(TAG, "[WEBRTC] onTrack: $trackKind")
                    if (trackKind == MediaStreamTrack.AUDIO_TRACK_KIND) {
                        Log.d(TAG, "[WEBRTC] Remote audio track received via onTrack")
                    }
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    Log.d(TAG, "[WEBRTC] Connection state changed: $newState")
                    if (newState != null) {
                        when (newState) {
                            PeerConnection.PeerConnectionState.NEW -> {
                                Log.d(TAG, "[WEBRTC] Connection: NEW - Initial state")
                            }
                            PeerConnection.PeerConnectionState.CONNECTING -> {
                                Log.d(TAG, "[WEBRTC] Connection: CONNECTING - Establishing connection")
                            }
                            PeerConnection.PeerConnectionState.CONNECTED -> {
                                Log.d(TAG, "[WEBRTC] Connection: CONNECTED - Connection established")
                            }
                            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                                Log.w(TAG, "[WEBRTC] Connection: DISCONNECTED - Connection lost")
                            }
                            PeerConnection.PeerConnectionState.FAILED -> {
                                Log.e(TAG, "[WEBRTC] Connection: FAILED - Connection failed")
                            }
                            PeerConnection.PeerConnectionState.CLOSED -> {
                                Log.d(TAG, "[WEBRTC] Connection: CLOSED - Connection closed")
                            }
                        }
                        Log.d(TAG, "[WEBRTC] Invoking connection state listener")
                        onConnectionStateChangeListener?.invoke(newState)
                    } else {
                        Log.w(TAG, "[WEBRTC] Connection state is NULL!")
                    }
                }
            }
        )

        if (peerConnection == null) {
            Log.e(TAG, "[WEBRTC] Failed to create peer connection!")
            return
        }
        
        Log.d(TAG, "[WEBRTC] Peer connection created successfully")
        
        // Add local audio track
        Log.d(TAG, "[WEBRTC] Adding local audio track...")
        addLocalAudioTrack()
        Log.d(TAG, "[WEBRTC] Local audio track added")
    }

    private fun addLocalAudioTrack() {
        Log.d(TAG, "addLocalAudioTrack: factory=${if (peerConnectionFactory == null) "NULL" else "NOT NULL"}, peerConnection=${if (peerConnection == null) "NULL" else "NOT NULL"}")
        
        if (peerConnectionFactory == null) {
            Log.e(TAG, "Cannot add audio track: peerConnectionFactory is null!")
            return
        }
        
        if (peerConnection == null) {
            Log.e(TAG, "Cannot add audio track: peerConnection is null!")
            return
        }
        
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        Log.d(TAG, "Creating audio source...")
        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        Log.d(TAG, "Creating audio track...")
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", audioSource)

        localAudioTrack?.let { track ->
            Log.d(TAG, "Audio track created, adding to peer connection...")
            track.setEnabled(true)
            val result = peerConnection?.addTrack(track, listOf("audio_stream"))
            Log.d(TAG, "Audio track added to peer connection, result: $result")
        } ?: run {
            Log.e(TAG, "Failed to create audio track!")
        }
    }

    fun createOffer(callback: (SessionDescription) -> Unit) {
        Log.d(TAG, "[OUTGOING] Creating offer...")
        if (peerConnection == null) {
            Log.e(TAG, "[OUTGOING] peerConnection is null!")
            return
        }
        
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let { sessionDescription ->
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "[OUTGOING] Offer set locally")
                            callback(sessionDescription)
                        }
                        override fun onCreateFailure(error: String?) {
                            Log.e(TAG, "[OUTGOING] Offer set failed: $error")
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "[OUTGOING] Offer set failed: $error")
                        }
                    }, sessionDescription)
                } ?: run {
                    Log.e(TAG, "[OUTGOING] SDP is null!")
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[OUTGOING] Offer creation failed: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "[OUTGOING] Offer creation failed: $error")
            }
        }, constraints)
    }

    fun createAnswer(callback: (SessionDescription) -> Unit) {
        Log.d(TAG, "[INCOMING] Creating answer...")
        if (peerConnection == null) {
            Log.e(TAG, "[INCOMING] Cannot create answer: peerConnection is null!")
            return
        }
        
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d(TAG, "[INCOMING] Answer created")
                sdp?.let { sessionDescription ->
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "[INCOMING] Answer set locally")
                            callback(sessionDescription)
                        }
                        override fun onCreateFailure(error: String?) {
                            Log.e(TAG, "[INCOMING] Answer set failed: $error")
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "[INCOMING] Answer set failed: $error")
                        }
                    }, sessionDescription)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[INCOMING] Answer creation failed: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "[INCOMING] Answer creation failed: $error")
            }
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription, callback: () -> Unit) {
        val prefix = if (sdp.type == org.webrtc.SessionDescription.Type.OFFER) "[INCOMING]" else "[OUTGOING]"
        Log.d(TAG, "$prefix Setting remote ${sdp.type}...")
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "$prefix Remote ${sdp.type} set")
                callback()
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "$prefix Remote ${sdp.type} set failed: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "$prefix Remote ${sdp.type} set failed: $error")
            }
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate): Boolean {
        val key = "${candidate.sdpMid}:${candidate.sdpMLineIndex}"
        if (peerConnection == null) {
            Log.e(TAG, "[WEBRTC] Cannot add ICE $key - no peer connection")
            return false
        }
        return try {
            peerConnection?.addIceCandidate(candidate) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "[WEBRTC] ICE add failed $key: ${e.message}")
            false
        }
    }

    fun setMicrophoneMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        audioManager?.setSpeakerphoneOn(enabled)
    }

    /**
     * Restart ICE gathering to handle network changes.
     * This triggers a new ICE candidate gathering process.
     */
    fun restartIce() {
        Log.d(TAG, "Restarting ICE gathering")
        peerConnection?.restartIce()
    }

    /**
     * Check if ICE gathering is complete.
     */
    fun isIceGatheringComplete(): Boolean {
        return iceGatheringState == PeerConnection.IceGatheringState.COMPLETE
    }
    
    /**
     * Log debug info about current WebRTC state.
     */
    fun logDebugInfo() {
        val pc = if (peerConnection != null) "OK" else "NULL"
        val stun = STUN_SERVERS.size
        val turn = customTurnServers.size
        Log.d(TAG, "[WEBRTC] Debug: PC=$pc, ICE=$iceGatheringState, Servers=$stun STUN + $turn TURN")
    }

    /**
     * Set listener for ICE gathering state changes.
     */
    fun setOnIceGatheringStateChangeListener(listener: (PeerConnection.IceGatheringState) -> Unit) {
        this.onIceGatheringStateChangeListener = listener
    }

    fun getPeerConnectionFactory(): PeerConnectionFactory? = peerConnectionFactory

    fun close() {
        Log.d(TAG, "Closing WebRTC connection")
        
        localAudioTrack?.setEnabled(false)
        localAudioTrack?.dispose()
        localAudioTrack = null

        audioSource?.dispose()
        audioSource = null

        peerConnection?.close()
        peerConnection = null

        audioManager?.stop()
        audioManager = null

        onIceCandidateListener = null
        onConnectionStateChangeListener = null
        onRemoteTrackListener = null
    }

    fun dispose() {
        close()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
    }
}
