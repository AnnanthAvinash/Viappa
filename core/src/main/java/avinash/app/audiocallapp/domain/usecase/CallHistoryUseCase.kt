package avinash.app.audiocallapp.domain.usecase

import avinash.app.audiocallapp.data.local.entity.CallHistoryEntity
import avinash.app.audiocallapp.data.repository.CallHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CallHistoryUseCase @Inject constructor(
    private val callHistoryRepository: CallHistoryRepository
) {
    suspend fun saveCallRecord(
        remoteUserId: String,
        remoteName: String,
        type: String,
        durationSeconds: Long
    ) {
        callHistoryRepository.insertCallRecord(remoteUserId, remoteName, type, durationSeconds)
    }

    fun getCallHistory(typeFilter: String? = null): Flow<List<CallHistoryEntity>> {
        return callHistoryRepository.getCallHistory(typeFilter)
    }

    fun getMissedCallCount(): Flow<Int> = callHistoryRepository.getMissedCallCount()

    suspend fun markMissedCallsSeen() = callHistoryRepository.markMissedCallsSeen()

    suspend fun getCallsWithUser(userId: String) = callHistoryRepository.getCallsWithUser(userId)
}
