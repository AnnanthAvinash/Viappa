package avinash.app.audiocallapp.data.repository

import android.util.Log
import avinash.app.audiocallapp.data.model.Connection
import avinash.app.audiocallapp.data.model.ConnectionRequest
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.domain.repository.ConnectionRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ConnectionRepository {

    companion object {
        private const val TAG = "ConnectionRepo"
    }

    private val usersCollection = firestore.collection("users")
    private val requestsCollection = firestore.collection("connection_requests")
    private val connectionsCollection = firestore.collection("connections")

    override fun searchUsers(query: String, currentUserId: String): Flow<List<User>> = callbackFlow {
        if (query.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val q = query.lowercase()
            val users = snapshot?.documents?.mapNotNull { doc ->
                doc.data?.let { User.fromMap(it, doc.id) }
            }?.filter {
                it.uniqueId != currentUserId &&
                    (it.displayName.lowercase().contains(q) || it.uniqueId.lowercase().contains(q))
            } ?: emptyList()
            trySend(users)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun sendRequest(fromId: String, fromName: String, toId: String, toName: String): Result<Unit> {
        return try {
            // Check forward direction (A→B)
            val existingForward = requestsCollection
                .whereEqualTo("senderId", fromId)
                .whereEqualTo("receiverId", toId)
                .get().await()
            val hasPendingForward = existingForward.documents.any {
                it.getString("status") == ConnectionRequest.STATUS_PENDING
            }
            if (hasPendingForward) return Result.failure(Exception("Request already sent"))

            // Check reverse direction (B→A)
            val existingReverse = requestsCollection
                .whereEqualTo("senderId", toId)
                .whereEqualTo("receiverId", fromId)
                .get().await()
            val hasPendingReverse = existingReverse.documents.any {
                it.getString("status") == ConnectionRequest.STATUS_PENDING
            }
            if (hasPendingReverse) return Result.failure(Exception("This user already sent you a request"))

            // Check if already friends
            val friendsA = connectionsCollection
                .whereEqualTo("userAId", fromId)
                .get().await()
            val friendsB = connectionsCollection
                .whereEqualTo("userBId", fromId)
                .get().await()
            val alreadyFriends = friendsA.documents.any { it.getString("userBId") == toId } ||
                friendsB.documents.any { it.getString("userAId") == toId }
            if (alreadyFriends) return Result.failure(Exception("Already friends"))

            val id = UUID.randomUUID().toString()
            val request = ConnectionRequest(
                id = id,
                senderId = fromId,
                receiverId = toId,
                senderName = fromName,
                receiverName = toName,
                status = ConnectionRequest.STATUS_PENDING,
                createdAt = Timestamp.now()
            )
            requestsCollection.document(id).set(request.toMap()).await()
            Log.d(TAG, "Request sent: $fromId -> $toId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendRequest failed", e)
            Result.failure(e)
        }
    }

    override suspend fun acceptRequest(request: ConnectionRequest): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val reqRef = requestsCollection.document(request.id)
                transaction.update(reqRef, "status", ConnectionRequest.STATUS_ACCEPTED)

                val connectionId = UUID.randomUUID().toString()
                val connRef = connectionsCollection.document(connectionId)
                val connection = Connection(
                    id = connectionId,
                    userAId = request.senderId,
                    userBId = request.receiverId,
                    userAName = request.senderName,
                    userBName = request.receiverName,
                    createdAt = Timestamp.now()
                )
                transaction.set(connRef, connection.toMap())
            }.await()
            Log.d(TAG, "Request accepted, connection created")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "acceptRequest failed", e)
            Result.failure(e)
        }
    }

    override suspend fun rejectRequest(requestId: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId)
                .update("status", ConnectionRequest.STATUS_REJECTED).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelRequest(requestId: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFriend(connectionId: String): Result<Unit> {
        return try {
            connectionsCollection.document(connectionId).delete().await()
            Log.d(TAG, "Friend removed: $connectionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeReceivedRequests(myId: String): Flow<List<ConnectionRequest>> = callbackFlow {
        val listener = requestsCollection
            .whereEqualTo("receiverId", myId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeReceivedRequests error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { ConnectionRequest.fromMap(it, doc.id) }
                }?.filter { it.status == ConnectionRequest.STATUS_PENDING }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0 }
                    ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    override fun observeSentRequests(myId: String): Flow<List<ConnectionRequest>> = callbackFlow {
        val listener = requestsCollection
            .whereEqualTo("senderId", myId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeSentRequests error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { ConnectionRequest.fromMap(it, doc.id) }
                }?.filter { it.status == ConnectionRequest.STATUS_PENDING }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0 }
                    ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    override fun observeFriends(myId: String): Flow<List<Connection>> {
        val flowA = callbackFlow {
            val listener = connectionsCollection
                .whereEqualTo("userAId", myId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                    val conns = snapshot?.documents?.mapNotNull { doc ->
                        doc.data?.let { Connection.fromMap(it, doc.id) }
                    } ?: emptyList()
                    trySend(conns)
                }
            awaitClose { listener.remove() }
        }
        val flowB = callbackFlow {
            val listener = connectionsCollection
                .whereEqualTo("userBId", myId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                    val conns = snapshot?.documents?.mapNotNull { doc ->
                        doc.data?.let { Connection.fromMap(it, doc.id) }
                    } ?: emptyList()
                    trySend(conns)
                }
            awaitClose { listener.remove() }
        }
        return combine(flowA, flowB) { a, b ->
            (a + b).distinctBy { it.id }
        }
    }
}
