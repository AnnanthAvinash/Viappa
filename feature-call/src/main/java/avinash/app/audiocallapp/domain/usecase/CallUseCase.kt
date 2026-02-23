package avinash.app.audiocallapp.domain.usecase

import avinash.app.audiocallapp.data.model.CallSignal
import avinash.app.audiocallapp.data.model.CallStatus
import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.repository.CallSignalingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CallUseCase @Inject constructor(
    private val callSignalingRepository: CallSignalingRepository,
    private val authRepository: AuthRepository
) {
    suspend fun initiateCall(
        callerId: String, calleeId: String,
        callerName: String, calleeName: String,
        offer: SessionDescription
    ): Result<String> {
        authRepository.updateUserStatus(UserStatus.IN_CALL)
        return callSignalingRepository.createCall(callerId, calleeId, callerName, calleeName, offer)
    }

    suspend fun answerCall(callId: String, answer: SessionDescription): Result<Unit> {
        authRepository.updateUserStatus(UserStatus.IN_CALL)
        return callSignalingRepository.answerCall(callId, answer)
    }

    suspend fun rejectCall(callId: String): Result<Unit> =
        callSignalingRepository.updateCallStatus(callId, CallStatus.REJECTED)

    suspend fun endCall(callId: String): Result<Unit> {
        authRepository.updateUserStatus(UserStatus.ONLINE)
        return callSignalingRepository.endCall(callId)
    }

    suspend fun addCallerIceCandidate(callId: String, candidate: IceCandidate): Result<Unit> =
        callSignalingRepository.addCallerIceCandidate(callId, candidate)

    suspend fun addCalleeIceCandidate(callId: String, candidate: IceCandidate): Result<Unit> =
        callSignalingRepository.addCalleeIceCandidate(callId, candidate)

    fun observeIncomingCalls(calleeId: String): Flow<CallSignal?> =
        callSignalingRepository.observeIncomingCalls(calleeId)

    fun observeCall(callId: String): Flow<CallSignal?> =
        callSignalingRepository.observeCall(callId)

    fun observeCallerIceCandidates(callId: String): Flow<List<IceCandidate>> =
        callSignalingRepository.observeCallerIceCandidates(callId)

    fun observeCalleeIceCandidates(callId: String): Flow<List<IceCandidate>> =
        callSignalingRepository.observeCalleeIceCandidates(callId)

    suspend fun cleanupCall(callId: String) {
        authRepository.updateUserStatus(UserStatus.ONLINE)
        callSignalingRepository.cleanupCall(callId)
    }
}
