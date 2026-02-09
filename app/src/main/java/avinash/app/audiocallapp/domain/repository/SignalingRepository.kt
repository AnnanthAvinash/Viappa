package avinash.app.audiocallapp.domain.repository

import avinash.app.audiocallapp.data.model.CallSignal
import avinash.app.audiocallapp.data.model.CallStatus
import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.User
import kotlinx.coroutines.flow.Flow

interface SignalingRepository {
    // User operations
    fun observeAvailableUsers(currentUserId: String): Flow<List<User>>
    
    // Call operations
    suspend fun createCall(
        callerId: String,
        calleeId: String,
        callerName: String,
        calleeName: String,
        offer: SessionDescription
    ): Result<String>
    
    suspend fun answerCall(callId: String, answer: SessionDescription): Result<Unit>
    suspend fun updateCallStatus(callId: String, status: CallStatus): Result<Unit>
    suspend fun endCall(callId: String): Result<Unit>
    
    // ICE candidates
    suspend fun addCallerIceCandidate(callId: String, candidate: IceCandidate): Result<Unit>
    suspend fun addCalleeIceCandidate(callId: String, candidate: IceCandidate): Result<Unit>
    
    // Observers
    fun observeIncomingCalls(calleeId: String): Flow<CallSignal?>
    fun observeCall(callId: String): Flow<CallSignal?>
    fun observeCallerIceCandidates(callId: String): Flow<List<IceCandidate>>
    fun observeCalleeIceCandidates(callId: String): Flow<List<IceCandidate>>
    
    // Cleanup
    suspend fun cleanupCall(callId: String)
}
