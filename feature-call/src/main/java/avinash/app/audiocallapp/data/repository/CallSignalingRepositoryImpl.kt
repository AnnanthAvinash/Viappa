package avinash.app.audiocallapp.data.repository

import avinash.app.audiocallapp.data.model.CallSignal
import avinash.app.audiocallapp.data.model.CallStatus
import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.domain.repository.CallSignalingRepository
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallSignalingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CallSignalingRepository {

    companion object {
        private const val TAG = "MOBILETAGFILTER"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    private val callsCollection = firestore.collection("calls")

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
                    Log.w(TAG, "$operationName failed (${attempt + 1}/$maxRetries): $msg. Retrying in ${delayMs}ms...")
                    delay(delayMs)
                } else {
                    Log.e(TAG, "$operationName failed after $maxRetries attempts: $msg", e)
                }
            }
        }
        return Result.failure(lastException ?: Exception("$operationName failed after $maxRetries attempts"))
    }

    override suspend fun createCall(
        callerId: String, calleeId: String,
        callerName: String, calleeName: String,
        offer: SessionDescription
    ): Result<String> {
        return retryOperation(operationName = "createCall") {
            val callId = UUID.randomUUID().toString()
            val callSignal = CallSignal(
                callId = callId, callerId = callerId, calleeId = calleeId,
                callerName = callerName, calleeName = calleeName,
                status = CallStatus.RINGING, offer = offer, createdAt = Timestamp.now()
            )
            callsCollection.document(callId).set(callSignal.toMap()).await()
            callId
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(Exception("Failed to create call: ${it.message}", it)) }
        )
    }

    override suspend fun answerCall(callId: String, answer: SessionDescription): Result<Unit> {
        return retryOperation(operationName = "answerCall") {
            callsCollection.document(callId).set(
                mapOf("answer" to answer.toMap(), "status" to CallStatus.ACCEPTED.name),
                SetOptions.merge()
            ).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(Exception("Failed to answer call: ${it.message}", it)) }
        )
    }

    override suspend fun updateCallStatus(callId: String, status: CallStatus): Result<Unit> {
        return retryOperation(operationName = "updateCallStatus") {
            callsCollection.document(callId).set(mapOf("status" to status.name), SetOptions.merge()).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(Exception("Failed to update call status: ${it.message}", it)) }
        )
    }

    override suspend fun endCall(callId: String): Result<Unit> {
        return retryOperation(operationName = "endCall", maxRetries = 5) {
            callsCollection.document(callId).set(mapOf("status" to CallStatus.ENDED.name), SetOptions.merge()).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.success(Unit) }
        )
    }

    override suspend fun addCallerIceCandidate(callId: String, candidate: IceCandidate): Result<Unit> {
        return retryOperation(maxRetries = 2, operationName = "addCallerIceCandidate") {
            callsCollection.document(callId).collection("callerCandidates").add(candidate.toMap()).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.success(Unit) }
        )
    }

    override suspend fun addCalleeIceCandidate(callId: String, candidate: IceCandidate): Result<Unit> {
        return retryOperation(maxRetries = 2, operationName = "addCalleeIceCandidate") {
            callsCollection.document(callId).collection("calleeCandidates").add(candidate.toMap()).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.success(Unit) }
        )
    }

    override fun observeIncomingCalls(calleeId: String): Flow<CallSignal?> = callbackFlow {
        val listener = callsCollection.whereEqualTo("calleeId", calleeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(null); return@addSnapshotListener }
                val call = snapshot?.documents
                    ?.mapNotNull { doc -> doc.data?.let { CallSignal.fromMap(it, doc.id) } }
                    ?.filter { it.status == CallStatus.RINGING }
                    ?.maxByOrNull { it.createdAt?.seconds ?: 0 }
                trySend(call)
            }
        awaitClose { listener.remove() }
    }

    override fun observeCall(callId: String): Flow<CallSignal?> = callbackFlow {
        val listener = callsCollection.document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(null); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.data?.let { CallSignal.fromMap(it, callId) })
                } else { trySend(null) }
            }
        awaitClose { listener.remove() }
    }

    override fun observeCallerIceCandidates(callId: String): Flow<List<IceCandidate>> = callbackFlow {
        val listener = callsCollection.document(callId).collection("callerCandidates")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.data?.let { d -> IceCandidate.fromMap(d) } } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun observeCalleeIceCandidates(callId: String): Flow<List<IceCandidate>> = callbackFlow {
        val listener = callsCollection.document(callId).collection("calleeCandidates")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.data?.let { d -> IceCandidate.fromMap(d) } } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun cleanupCall(callId: String) {
        retryOperation(maxRetries = 5, operationName = "cleanupCall") {
            listOf("callerCandidates", "calleeCandidates").forEach { sub ->
                val docs = callsCollection.document(callId).collection(sub).get().await()
                for (doc in docs.documents) {
                    try { doc.reference.delete().await() } catch (_: Exception) {}
                }
            }
            callsCollection.document(callId).delete().await()
        }
    }
}
