package avinash.app.audiocallapp.walkietalkie

import android.util.Log
import org.webrtc.*
import java.util.concurrent.ConcurrentHashMap

data class PeerConnectionEntry(
    val peerConnection: PeerConnection,
    val localAudioTrack: AudioTrack,
    val audioSource: AudioSource,
    var dataChannel: DataChannel? = null,
    var remoteDataChannel: DataChannel? = null,
    var isRemoteTalking: Boolean = false
)

class PeerConnectionPool(
    private val factory: PeerConnectionFactory,
    private val iceServers: List<PeerConnection.IceServer>
) {
    companion object {
        private const val TAG = "WT_POOL"
    }

    private val connections = ConcurrentHashMap<String, PeerConnectionEntry>()

    val activeConnectionIds: Set<String> get() = connections.keys.toSet()

    fun getEntry(friendId: String): PeerConnectionEntry? = connections[friendId]

    fun hasConnection(friendId: String): Boolean = connections.containsKey(friendId)

    fun createConnection(
        friendId: String,
        onIceCandidate: (String, IceCandidate) -> Unit,
        onConnectionStateChange: (String, PeerConnection.PeerConnectionState) -> Unit,
        onRemoteTrack: (String, MediaStreamTrack) -> Unit,
        onRemoteTalkStateChange: (String, Boolean) -> Unit
    ): PeerConnectionEntry? {
        if (connections.containsKey(friendId)) {
            Log.w(TAG, "Connection already exists for $friendId, closing old one")
            closeConnection(friendId)
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
        }

        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "[$friendId] Signaling: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "[$friendId] ICE connection: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "[$friendId] ICE gathering: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { onIceCandidate(friendId, it) }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}

            override fun onDataChannel(channel: DataChannel?) {
                channel?.let { dc ->
                    Log.d(TAG, "[$friendId] Remote data channel received")
                    connections[friendId]?.let { entry ->
                        entry.remoteDataChannel = dc
                        dc.registerObserver(createDataChannelObserver(friendId, onRemoteTalkStateChange))
                    }
                }
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "[$friendId] Renegotiation needed")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                receiver?.track()?.let { track ->
                    if (track.kind() == MediaStreamTrack.AUDIO_TRACK_KIND) {
                        Log.d(TAG, "[$friendId] Remote audio track added")
                        onRemoteTrack(friendId, track)
                    }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "[$friendId] Connection: $newState")
                newState?.let { onConnectionStateChange(friendId, it) }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {}
        }

        val peerConnection = factory.createPeerConnection(rtcConfig, observer)
        if (peerConnection == null) {
            Log.e(TAG, "[$friendId] Failed to create peer connection")
            return null
        }

        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        val audioSource = factory.createAudioSource(audioConstraints)
        val audioTrack = factory.createAudioTrack("wt_audio_$friendId", audioSource)
        audioTrack.setEnabled(false) // OFF by default — enabled only on push-to-talk

        peerConnection.addTrack(audioTrack, listOf("wt_stream_$friendId"))

        val dcInit = DataChannel.Init().apply { ordered = true }
        val dataChannel = peerConnection.createDataChannel("wt_talk_state", dcInit)

        val entry = PeerConnectionEntry(
            peerConnection = peerConnection,
            localAudioTrack = audioTrack,
            audioSource = audioSource,
            dataChannel = dataChannel
        )
        connections[friendId] = entry
        Log.d(TAG, "[$friendId] Connection created (audio OFF, DC ready)")
        return entry
    }

    private fun createDataChannelObserver(
        friendId: String,
        onRemoteTalkStateChange: (String, Boolean) -> Unit
    ): DataChannel.Observer {
        return object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}

            override fun onStateChange() {}

            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let {
                    val bytes = ByteArray(it.data.remaining())
                    it.data.get(bytes)
                    val message = String(bytes)
                    Log.d(TAG, "[$friendId] DataChannel message: $message")
                    when (message) {
                        "TALK_START" -> {
                            connections[friendId]?.isRemoteTalking = true
                            onRemoteTalkStateChange(friendId, true)
                        }
                        "TALK_STOP" -> {
                            connections[friendId]?.isRemoteTalking = false
                            onRemoteTalkStateChange(friendId, false)
                        }
                    }
                }
            }
        }
    }

    fun enableAudio(friendId: String) {
        connections[friendId]?.let { entry ->
            entry.localAudioTrack.setEnabled(true)
            sendDataChannelMessage(friendId, "TALK_START")
            Log.d(TAG, "[$friendId] Audio ENABLED (talking)")
        }
    }

    fun disableAudio(friendId: String) {
        connections[friendId]?.let { entry ->
            entry.localAudioTrack.setEnabled(false)
            sendDataChannelMessage(friendId, "TALK_STOP")
            Log.d(TAG, "[$friendId] Audio DISABLED (stopped talking)")
        }
    }

    fun disableAllAudio() {
        connections.forEach { (friendId, entry) ->
            entry.localAudioTrack.setEnabled(false)
            sendDataChannelMessage(friendId, "TALK_STOP")
        }
    }

    private fun sendDataChannelMessage(friendId: String, message: String) {
        try {
            val dc = connections[friendId]?.dataChannel
            if (dc?.state() == DataChannel.State.OPEN) {
                val buffer = DataChannel.Buffer(
                    java.nio.ByteBuffer.wrap(message.toByteArray()),
                    false
                )
                dc.send(buffer)
            }
        } catch (e: Exception) {
            Log.w(TAG, "[$friendId] Failed to send DC message: ${e.message}")
        }
    }

    fun createOffer(friendId: String, callback: (SessionDescription) -> Unit) {
        val pc = connections[friendId]?.peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let { desc ->
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() { callback(desc) }
                        override fun onCreateFailure(error: String?) {
                            Log.e(TAG, "[$friendId] Set local offer failed: $error")
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "[$friendId] Set local offer failed: $error")
                        }
                    }, desc)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[$friendId] Create offer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun createAnswer(friendId: String, callback: (SessionDescription) -> Unit) {
        val pc = connections[friendId]?.peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let { desc ->
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() { callback(desc) }
                        override fun onCreateFailure(error: String?) {
                            Log.e(TAG, "[$friendId] Set local answer failed: $error")
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "[$friendId] Set local answer failed: $error")
                        }
                    }, desc)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[$friendId] Create answer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun setRemoteDescription(friendId: String, sdp: SessionDescription, callback: () -> Unit) {
        val pc = connections[friendId]?.peerConnection ?: return
        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() { callback() }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[$friendId] Set remote desc failed: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "[$friendId] Set remote desc failed: $error")
            }
        }, sdp)
    }

    fun addIceCandidate(friendId: String, candidate: IceCandidate): Boolean {
        val pc = connections[friendId]?.peerConnection ?: return false
        return try {
            pc.addIceCandidate(candidate)
        } catch (e: Exception) {
            Log.w(TAG, "[$friendId] Add ICE failed: ${e.message}")
            false
        }
    }

    fun restartIce(friendId: String) {
        connections[friendId]?.peerConnection?.restartIce()
        Log.d(TAG, "[$friendId] ICE restart triggered")
    }

    fun restartAllIce() {
        connections.forEach { (id, _) -> restartIce(id) }
    }

    fun closeConnection(friendId: String) {
        connections.remove(friendId)?.let { entry ->
            try {
                entry.dataChannel?.close()
                entry.remoteDataChannel?.close()
                entry.localAudioTrack.setEnabled(false)
                entry.localAudioTrack.dispose()
                entry.audioSource.dispose()
                entry.peerConnection.close()
                Log.d(TAG, "[$friendId] Connection closed and disposed")
            } catch (e: Exception) {
                Log.e(TAG, "[$friendId] Error closing connection: ${e.message}")
            }
        }
    }

    fun closeAll() {
        val ids = connections.keys.toList()
        ids.forEach { closeConnection(it) }
        Log.d(TAG, "All connections closed (${ids.size})")
    }

    fun getConnectionState(friendId: String): PeerConnection.PeerConnectionState? {
        return connections[friendId]?.peerConnection?.connectionState()
    }

    fun isConnected(friendId: String): Boolean {
        return getConnectionState(friendId) == PeerConnection.PeerConnectionState.CONNECTED
    }

    val connectionCount: Int get() = connections.size
}
