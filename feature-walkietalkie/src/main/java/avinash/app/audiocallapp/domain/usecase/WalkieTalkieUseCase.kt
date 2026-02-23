package avinash.app.audiocallapp.domain.usecase

import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.WtConnectionSignal
import avinash.app.audiocallapp.data.model.WtConnectionStatus
import avinash.app.audiocallapp.domain.repository.WalkieTalkieRepository
import avinash.app.audiocallapp.domain.repository.WtRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WalkieTalkieUseCase @Inject constructor(
    private val repository: WalkieTalkieRepository
) {
    suspend fun createOffer(
        pairId: String,
        offererId: String,
        answererId: String,
        offererName: String,
        answererName: String,
        offer: SessionDescription
    ): Result<Unit> = repository.createOffer(pairId, offererId, answererId, offererName, answererName, offer)

    suspend fun updateAnswer(pairId: String, answer: SessionDescription): Result<Unit> =
        repository.updateAnswer(pairId, answer)

    suspend fun updateStatus(pairId: String, status: WtConnectionStatus): Result<Unit> =
        repository.updateStatus(pairId, status)

    suspend fun addIceCandidate(pairId: String, role: WtRole, candidate: IceCandidate): Result<Unit> =
        repository.addIceCandidate(pairId, role, candidate)

    fun observeConnection(pairId: String): Flow<WtConnectionSignal?> =
        repository.observeConnection(pairId)

    fun observeIceCandidates(pairId: String, role: WtRole): Flow<List<IceCandidate>> =
        repository.observeIceCandidates(pairId, role)

    suspend fun cleanupConnection(pairId: String) =
        repository.cleanupConnection(pairId)

    suspend fun cleanupAllConnections(userId: String) =
        repository.cleanupAllConnections(userId)
}
