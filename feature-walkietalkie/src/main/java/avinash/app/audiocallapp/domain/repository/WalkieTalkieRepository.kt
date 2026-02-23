package avinash.app.audiocallapp.domain.repository

import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.WtConnectionSignal
import avinash.app.audiocallapp.data.model.WtConnectionStatus
import kotlinx.coroutines.flow.Flow

enum class WtRole { OFFERER, ANSWERER }

interface WalkieTalkieRepository {
    suspend fun createOffer(
        pairId: String,
        offererId: String,
        answererId: String,
        offererName: String,
        answererName: String,
        offer: SessionDescription
    ): Result<Unit>

    suspend fun updateAnswer(pairId: String, answer: SessionDescription): Result<Unit>

    suspend fun updateStatus(pairId: String, status: WtConnectionStatus): Result<Unit>

    suspend fun addIceCandidate(pairId: String, role: WtRole, candidate: IceCandidate): Result<Unit>

    fun observeConnection(pairId: String): Flow<WtConnectionSignal?>

    fun observeMyConnections(userId: String): Flow<List<WtConnectionSignal>>

    fun observeIceCandidates(pairId: String, role: WtRole): Flow<List<IceCandidate>>

    suspend fun cleanupConnection(pairId: String)

    suspend fun cleanupAllConnections(userId: String)
}
