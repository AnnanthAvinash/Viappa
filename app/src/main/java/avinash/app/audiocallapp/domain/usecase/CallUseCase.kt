package avinash.app.audiocallapp.domain.usecase

import avinash.app.audiocallapp.data.model.CallSignal
import avinash.app.audiocallapp.data.model.CallStatus
import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.repository.SignalingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CallUseCase @Inject constructor(
    private val signalingRepository: SignalingRepository,
    private val authRepository: AuthRepository
) {
    suspend fun initiateCall(
        callerId: String,
        calleeId: String,
        callerName: String,
        calleeName: String,
        offer: SessionDescription
    ): Result<String> {
        authRepository.updateUserStatus(UserStatus.IN_CALL)
        val result = signalingRepository.createCall(callerId, calleeId, callerName, calleeName, offer)
        return result
    }

    suspend fun answerCall(callId: String, answer: SessionDescription): Result<Unit> {
        android.util.Log.d("AudioCallApp", "[INCOMING] Answering call: $callId")
        authRepository.updateUserStatus(UserStatus.IN_CALL)
        val result = signalingRepository.answerCall(callId, answer)
        if (result.isSuccess) {
            android.util.Log.d("AudioCallApp", "[INCOMING] Answer sent")
        } else {
            android.util.Log.e("AudioCallApp", "[INCOMING] Answer failed: ${result.exceptionOrNull()?.message}")
        }
        return result
    }

    suspend fun rejectCall(callId: String): Result<Unit> {
        android.util.Log.d("AudioCallApp", "[INCOMING] Rejecting call: $callId")
        return signalingRepository.updateCallStatus(callId, CallStatus.REJECTED)
    }

    suspend fun endCall(callId: String): Result<Unit> {
        android.util.Log.d("AudioCallApp", "Ending call: $callId")
        authRepository.updateUserStatus(UserStatus.ONLINE)
        return signalingRepository.endCall(callId)
    }

    suspend fun addCallerIceCandidate(callId: String, candidate: IceCandidate): Result<Unit> {
        return signalingRepository.addCallerIceCandidate(callId, candidate)
    }

    suspend fun addCalleeIceCandidate(callId: String, candidate: IceCandidate): Result<Unit> {
        return signalingRepository.addCalleeIceCandidate(callId, candidate)
    }

    fun observeIncomingCalls(calleeId: String): Flow<CallSignal?> {
        return signalingRepository.observeIncomingCalls(calleeId)
    }

    fun observeCall(callId: String): Flow<CallSignal?> {
        return signalingRepository.observeCall(callId)
    }

    fun observeCallerIceCandidates(callId: String): Flow<List<IceCandidate>> {
        return signalingRepository.observeCallerIceCandidates(callId)
    }

    fun observeCalleeIceCandidates(callId: String): Flow<List<IceCandidate>> {
        return signalingRepository.observeCalleeIceCandidates(callId)
    }

    suspend fun cleanupCall(callId: String) {
        authRepository.updateUserStatus(UserStatus.ONLINE)
        signalingRepository.cleanupCall(callId)
    }
}
