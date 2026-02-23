package avinash.app.audiocallapp.data.repository

import android.util.Log
import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.WtConnectionSignal
import avinash.app.audiocallapp.data.model.WtConnectionStatus
import avinash.app.audiocallapp.domain.repository.WalkieTalkieRepository
import avinash.app.audiocallapp.domain.repository.WtRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalkieTalkieRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : WalkieTalkieRepository {

    companion object {
        private const val TAG = "WT_REPO"
        private const val COLLECTION = "wt_connections"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    private val wtCollection = firestore.collection(COLLECTION)

    private suspend fun <T> retryOperation(
        maxRetries: Int = MAX_RETRIES,
        operationName: String = "operation",
        operation: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                val msg = when (e) {
                    is FirebaseFirestoreException -> "Firestore ${e.code}: ${e.message}"
                    else -> e.message ?: "Unknown error"
                }
                if (attempt < maxRetries - 1) {
                    val delayMs = RETRY_DELAY_MS * (1 shl attempt)
                    Log.w(TAG, "$operationName failed (${attempt + 1}/$maxRetries): $msg. Retry in ${delayMs}ms")
                    delay(delayMs)
                } else {
                    Log.e(TAG, "$operationName failed after $maxRetries attempts: $msg", e)
                }
            }
        }
        return Result.failure(lastException ?: Exception("$operationName failed"))
    }

    override suspend fun createOffer(
        pairId: String,
        offererId: String,
        answererId: String,
        offererName: String,
        answererName: String,
        offer: SessionDescription
    ): Result<Unit> {
        Log.d(TAG, "Creating offer: $pairId ($offererId -> $answererId)")
        return retryOperation(operationName = "createOffer") {
            val signal = WtConnectionSignal(
                pairId = pairId,
                offererId = offererId,
                answererId = answererId,
                offererName = offererName,
                answererName = answererName,
                offer = offer,
                answer = null,
                status = WtConnectionStatus.OFFERING,
                createdAt = Timestamp.now()
            )
            wtCollection.document(pairId).set(signal.toMap()).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun updateAnswer(pairId: String, answer: SessionDescription): Result<Unit> {
        Log.d(TAG, "Updating answer: $pairId")
        return retryOperation(operationName = "updateAnswer") {
            wtCollection.document(pairId).set(
                mapOf(
                    "answer" to answer.toMap(),
                    "status" to WtConnectionStatus.CONNECTED.name
                ),
                SetOptions.merge()
            ).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun updateStatus(pairId: String, status: WtConnectionStatus): Result<Unit> {
        return retryOperation(operationName = "updateStatus") {
            wtCollection.document(pairId).set(
                mapOf("status" to status.name),
                SetOptions.merge()
            ).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun addIceCandidate(
        pairId: String,
        role: WtRole,
        candidate: IceCandidate
    ): Result<Unit> {
        val subCollection = if (role == WtRole.OFFERER) "offererCandidates" else "answererCandidates"
        return retryOperation(maxRetries = 2, operationName = "addIceCandidate") {
            wtCollection.document(pairId)
                .collection(subCollection)
                .add(candidate.toMap())
                .await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = {
                Log.w(TAG, "Failed to add ICE candidate: ${it.message}")
                Result.success(Unit)
            }
        )
    }

    override fun observeConnection(pairId: String): Flow<WtConnectionSignal?> = callbackFlow {
        val listener = wtCollection.document(pairId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing connection $pairId: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val signal = snapshot.data?.let { WtConnectionSignal.fromMap(it, pairId) }
                    trySend(signal)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeMyConnections(userId: String): Flow<List<WtConnectionSignal>> = callbackFlow {
        val listenerOfferer = wtCollection
            .whereEqualTo("offererId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing offerer connections: ${error.message}")
                    return@addSnapshotListener
                }
                val offererSignals = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { WtConnectionSignal.fromMap(it, doc.id) }
                } ?: emptyList()

                val answererListener = wtCollection
                    .whereEqualTo("answererId", userId)
                    .addSnapshotListener inner@{ snap2, err2 ->
                        if (err2 != null) {
                            trySend(offererSignals)
                            return@inner
                        }
                        val answererSignals = snap2?.documents?.mapNotNull { doc ->
                            doc.data?.let { WtConnectionSignal.fromMap(it, doc.id) }
                        } ?: emptyList()
                        trySend(offererSignals + answererSignals)
                    }
            }
        awaitClose { listenerOfferer.remove() }
    }

    override fun observeIceCandidates(pairId: String, role: WtRole): Flow<List<IceCandidate>> = callbackFlow {
        val subCollection = if (role == WtRole.OFFERER) "offererCandidates" else "answererCandidates"
        val listener = wtCollection.document(pairId)
            .collection(subCollection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val candidates = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { IceCandidate.fromMap(it) }
                } ?: emptyList()
                trySend(candidates)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun cleanupConnection(pairId: String) {
        retryOperation(maxRetries = 5, operationName = "cleanupConnection") {
            listOf("offererCandidates", "answererCandidates").forEach { sub ->
                val docs = wtCollection.document(pairId).collection(sub).get().await()
                for (doc in docs.documents) {
                    try { doc.reference.delete().await() } catch (_: Exception) {}
                }
            }
            wtCollection.document(pairId).delete().await()
            Log.d(TAG, "Cleaned up connection: $pairId")
        }
    }

    override suspend fun cleanupAllConnections(userId: String) {
        try {
            val offererDocs = wtCollection.whereEqualTo("offererId", userId).get().await()
            val answererDocs = wtCollection.whereEqualTo("answererId", userId).get().await()
            val allDocs = offererDocs.documents + answererDocs.documents
            for (doc in allDocs) {
                try { cleanupConnection(doc.id) } catch (_: Exception) {}
            }
            Log.d(TAG, "Cleaned up all connections for user: $userId (${allDocs.size} docs)")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up all connections: ${e.message}", e)
        }
    }
}
