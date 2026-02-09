package avinash.app.audiocallapp.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class RTCAudioManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioCallApp"
    }

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var savedAudioMode: Int = AudioManager.MODE_NORMAL
    private var savedIsSpeakerPhoneOn: Boolean = false
    private var savedIsMicrophoneMute: Boolean = false

    fun start() {
        Log.d(TAG, "Starting audio manager")
        
        // Save current audio state
        savedAudioMode = audioManager.mode
        savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn
        savedIsMicrophoneMute = audioManager.isMicrophoneMute

        // Request audio focus
        requestAudioFocus()

        // Set audio mode for voice communication
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false
        audioManager.isSpeakerphoneOn = false // Default to earpiece
    }

    fun stop() {
        Log.d(TAG, "Stopping audio manager")
        
        // Restore previous audio state
        audioManager.mode = savedAudioMode
        audioManager.isSpeakerphoneOn = savedIsSpeakerPhoneOn
        audioManager.isMicrophoneMute = savedIsMicrophoneMute

        // Abandon audio focus
        abandonAudioFocus()
    }

    fun setSpeakerphoneOn(on: Boolean) {
        Log.d(TAG, "setSpeakerphoneOn: $on")
        audioManager.isSpeakerphoneOn = on
    }

    fun isSpeakerphoneOn(): Boolean {
        return audioManager.isSpeakerphoneOn
    }

    fun setMicrophoneMute(mute: Boolean) {
        Log.d(TAG, "setMicrophoneMute: $mute")
        audioManager.isMicrophoneMute = mute
    }

    fun isMicrophoneMute(): Boolean {
        return audioManager.isMicrophoneMute
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d(TAG, "Audio focus changed: $focusChange")
                }
                .build()

            audioFocusRequest?.let {
                audioManager.requestAudioFocus(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange -> Log.d(TAG, "Audio focus changed: $focusChange") },
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
