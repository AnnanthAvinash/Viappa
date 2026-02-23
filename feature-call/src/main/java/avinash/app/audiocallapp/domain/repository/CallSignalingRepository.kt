package avinash.app.audiocallapp.domain.repository

import avinash.app.audiocallapp.data.model.CallSignal
import avinash.app.audiocallapp.data.model.CallStatus
import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import kotlinx.coroutines.flow.Flow

interface CallSignalingRepository {
    suspend fun createCall(
        callerId: String, calleeId: String,
        callerName: String, calleeName: String,
        offer: SessionDescription
    ): Result<String>

    suspend fun answerCall(callId: String, answer: SessionDescription): Result<Unit>
    suspend fun updateCallStatus(callId: String, status: CallStatus): Result<Unit>
    suspend fun endCall(callId: String): Result<Unit>

    suspend fun addCallerIceCandidate(callId: String, candidate: IceCandidate): Result<Unit>
    suspend fun addCalleeIceCandidate(callId: String, candidate: IceCandidate): Result<Unit>

    fun observeIncomingCalls(calleeId: String): Flow<CallSignal?>
    fun observeCall(callId: String): Flow<CallSignal?>
    fun observeCallerIceCandidates(callId: String): Flow<List<IceCandidate>>
    fun observeCalleeIceCandidates(callId: String): Flow<List<IceCandidate>>

    suspend fun cleanupCall(callId: String)
}
