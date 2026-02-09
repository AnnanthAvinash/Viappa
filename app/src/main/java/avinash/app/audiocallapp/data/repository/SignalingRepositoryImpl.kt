package avinash.app.audiocallapp.data.repository

import avinash.app.audiocallapp.data.model.CallSignal
import avinash.app.audiocallapp.data.model.CallStatus
import avinash.app.audiocallapp.data.model.IceCandidate
import avinash.app.audiocallapp.data.model.SessionDescription
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.domain.repository.SignalingRepository
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
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
class SignalingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SignalingRepository {

    companion object {
        private const val TAG = "MOBILETAGFILTER"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    private val usersCollection = firestore.collection("users")
    private val callsCollection = firestore.collection("calls")

    /**
     * Retry a Firestore operation with exponential backoff.
     */
    private suspend fun <T> retryOperation(
        maxRetries: Int = MAX_RETRIES,
        operationName: String = "operation",
        operation: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                if (attempt > 0) {
                    Log.d(TAG, "$operationName succeeded after ${attempt + 1} attempts")
                }
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                val errorMessage = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            FirebaseFirestoreException.Code.UNAVAILABLE -> "Service unavailable"
                            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "Request timeout"
                            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Permission denied"
                            else -> "Firestore error: ${e.code}"
                        }
                    }
                    else -> e.message ?: "Unknown error"
                }
                
                if (attempt < maxRetries - 1) {
                    val delayMs = RETRY_DELAY_MS * (1 shl attempt) // Exponential backoff
                    Log.w(TAG, "$operationName failed (attempt ${attempt + 1}/$maxRetries): $errorMessage. Retrying in ${delayMs}ms...")
                    delay(delayMs)
                } else {
                    Log.e(TAG, "$operationName failed after $maxRetries attempts: $errorMessage", e)
                }
            }
        }
        
        return Result.failure(
            lastException ?: Exception("$operationName failed after $maxRetries attempts")
        )
    }

    override fun observeAvailableUsers(currentUserId: String): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .whereIn("status", listOf(UserStatus.ONLINE.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing available users: ${error.message}", error)
                    // Don't close the flow, just send empty list to allow recovery
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { data ->
                        User.fromMap(data, doc.id)
                    }
                }?.filter { it.uniqueId != currentUserId } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createCall(
        callerId: String,
        calleeId: String,
        callerName: String,
        calleeName: String,
        offer: SessionDescription
    ): Result<String> {
        Log.d(TAG, "[OUTGOING] Creating call: $callerName -> $calleeName")
        return retryOperation(
            operationName = "createCall"
        ) {
            val callId = UUID.randomUUID().toString()
            val callSignal = CallSignal(
                callId = callId,
                callerId = callerId,
                calleeId = calleeId,
                callerName = callerName,
                calleeName = calleeName,
                status = CallStatus.RINGING,
                offer = offer,
                createdAt = Timestamp.now()
            )

            callsCollection.document(callId).set(callSignal.toMap()).await()
            Log.d(TAG, "[OUTGOING] Call saved: $callId")
            callId
        }.fold(
            onSuccess = { callId -> 
                Result.success(callId) 
            },
            onFailure = { e -> 
                Log.e(TAG, "[OUTGOING] Save failed: ${e.message}")
                Result.failure(Exception("Failed to create call: ${e.message}", e))
            }
        )
    }

    override suspend fun answerCall(callId: String, answer: SessionDescription): Result<Unit> {
        Log.d(TAG, "[INCOMING] Saving answer to Firestore: $callId")
        return retryOperation(
            operationName = "answerCall"
        ) {
            callsCollection.document(callId).set(
                mapOf(
                    "answer" to answer.toMap(),
                    "status" to CallStatus.ACCEPTED.name
                ),
                SetOptions.merge()
            ).await()
            Log.d(TAG, "[INCOMING] Answer saved")
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e -> 
                Log.e(TAG, "[INCOMING] Answer save failed: ${e.message}")
                Result.failure(Exception("Failed to answer call: ${e.message}", e))
            }
        )
    }

    override suspend fun updateCallStatus(callId: String, status: CallStatus): Result<Unit> {
        Log.d(TAG, "Updating call status: $callId -> $status")
        return retryOperation(
            operationName = "updateCallStatus"
        ) {
            callsCollection.document(callId).set(
                mapOf("status" to status.name),
                SetOptions.merge()
            ).await()
        }.fold(
            onSuccess = { 
                Log.d(TAG, "Call status updated: $status")
                Result.success(Unit) 
            },
            onFailure = { e -> 
                Log.e(TAG, "Failed to update call status: ${e.message}")
                Result.failure(Exception("Failed to update call status: ${e.message}", e))
            }
        )
    }

    override suspend fun endCall(callId: String): Result<Unit> {
        return retryOperation(
            operationName = "endCall",
            maxRetries = 5 // More retries for critical cleanup operation
        ) {
            callsCollection.document(callId).set(
                mapOf("status" to CallStatus.ENDED.name),
                SetOptions.merge()
            ).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e -> 
                Log.e(TAG, "Failed to end call $callId: ${e.message}", e)
                // Still return success to prevent blocking cleanup
                Result.success(Unit)
            }
        )
    }

    override suspend fun addCallerIceCandidate(callId: String, candidate: IceCandidate): Result<Unit> {
        return retryOperation(
            operationName = "addCallerIceCandidate",
            maxRetries = 2 // Fewer retries for ICE candidates (they come frequently)
        ) {
            callsCollection.document(callId)
                .collection("callerCandidates")
                .add(candidate.toMap())
                .await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e -> 
                // Log but don't fail - ICE candidates are best effort
                Log.w(TAG, "Failed to add caller ICE candidate: ${e.message}")
                Result.success(Unit)
            }
        )
    }

    override suspend fun addCalleeIceCandidate(callId: String, candidate: IceCandidate): Result<Unit> {
        return retryOperation(
            operationName = "addCalleeIceCandidate",
            maxRetries = 2 // Fewer retries for ICE candidates (they come frequently)
        ) {
            callsCollection.document(callId)
                .collection("calleeCandidates")
                .add(candidate.toMap())
                .await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e -> 
                // Log but don't fail - ICE candidates are best effort
                Log.w(TAG, "Failed to add callee ICE candidate: ${e.message}")
                Result.success(Unit)
            }
        )
    }

    override fun observeIncomingCalls(calleeId: String): Flow<CallSignal?> = callbackFlow {
        Log.d(TAG, "[INCOMING] Listening for calls (calleeId: $calleeId)")
        val listener = callsCollection
            .whereEqualTo("calleeId", calleeId)
            .whereEqualTo("status", CallStatus.RINGING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "[INCOMING] Query error: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }

                val call = snapshot?.documents?.firstOrNull()?.let { doc ->
                    doc.data?.let { CallSignal.fromMap(it, doc.id) }
                }
                
                if (call != null) {
                    Log.d(TAG, "[INCOMING] Call received: ${call.callerName} (${call.callerId})")
                }
                
                trySend(call)
            }

        awaitClose { 
            Log.d(TAG, "[INCOMING] Stopped listening")
            listener.remove() 
        }
    }

    override fun observeCall(callId: String): Flow<CallSignal?> = callbackFlow {
        val listener = callsCollection.document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val call = snapshot.data?.let { CallSignal.fromMap(it, callId) }
                    trySend(call)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observeCallerIceCandidates(callId: String): Flow<List<IceCandidate>> = callbackFlow {
        val listener = callsCollection.document(callId)
            .collection("callerCandidates")
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

    override fun observeCalleeIceCandidates(callId: String): Flow<List<IceCandidate>> = callbackFlow {
        val listener = callsCollection.document(callId)
            .collection("calleeCandidates")
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

    override suspend fun cleanupCall(callId: String) {
        retryOperation(
            operationName = "cleanupCall",
            maxRetries = 5 // More retries for critical cleanup
        ) {
            // Delete ICE candidates subcollections
            val callerCandidates = callsCollection.document(callId)
                .collection("callerCandidates").get().await()
            for (doc in callerCandidates.documents) {
                retryOperation(
                    operationName = "deleteCallerCandidate",
                    maxRetries = 2
                ) {
                    doc.reference.delete().await()
                }
            }

            val calleeCandidates = callsCollection.document(callId)
                .collection("calleeCandidates").get().await()
            for (doc in calleeCandidates.documents) {
                retryOperation(
                    operationName = "deleteCalleeCandidate",
                    maxRetries = 2
                ) {
                    doc.reference.delete().await()
                }
            }

            // Delete call document
            callsCollection.document(callId).delete().await()
            Log.d(TAG, "Successfully cleaned up call: $callId")
        }.fold(
            onSuccess = { /* Cleanup successful */ },
            onFailure = { e ->
                Log.e(TAG, "Failed to cleanup call $callId after retries: ${e.message}", e)
                // Log but don't throw - cleanup is best effort
            }
        )
    }
}
