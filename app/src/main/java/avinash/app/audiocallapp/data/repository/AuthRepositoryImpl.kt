package avinash.app.audiocallapp.data.repository

import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.domain.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun checkUniqueIdAvailability(uniqueId: String): Boolean {
        return try {
            val doc = usersCollection.document(uniqueId).get().await()
            !doc.exists()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun registerUser(uniqueId: String, displayName: String): Result<User> {
        return try {
            // Sign in anonymously first
            val authResult = firebaseAuth.signInAnonymously().await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to create anonymous user")

            // Check if uniqueId is still available
            val isAvailable = checkUniqueIdAvailability(uniqueId)
            if (!isAvailable) {
                firebaseAuth.currentUser?.delete()?.await()
                return Result.failure(Exception("Username already taken"))
            }

            // Create user document
            val user = User(
                uniqueId = uniqueId,
                displayName = displayName.ifBlank { uniqueId },
                status = UserStatus.ONLINE,
                lastSeen = Timestamp.now(),
                createdAt = Timestamp.now()
            )

            usersCollection.document(uniqueId).set(user.toMap()).await()

            // Store the mapping of Firebase UID to uniqueId
            firestore.collection("uid_mapping").document(uid)
                .set(mapOf("uniqueId" to uniqueId)).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return try {
            val mappingDoc = firestore.collection("uid_mapping").document(uid).get().await()
            val uniqueId = mappingDoc.getString("uniqueId") ?: return null
            val userDoc = usersCollection.document(uniqueId).get().await()
            if (userDoc.exists()) {
                userDoc.data?.let { User.fromMap(it, uniqueId) }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateUserStatus(status: UserStatus) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val mappingDoc = firestore.collection("uid_mapping").document(uid).get().await()
            val uniqueId = mappingDoc.getString("uniqueId") ?: return
            
            usersCollection.document(uniqueId).set(
                mapOf(
                    "status" to status.name,
                    "lastSeen" to Timestamp.now()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            // Handle silently
        }
    }

    override suspend fun signOut() {
        updateUserStatus(UserStatus.OFFLINE)
        firebaseAuth.signOut()
    }

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val mappingDoc = firestore.collection("uid_mapping").document(uid).get().await()
        val uniqueId = mappingDoc.getString("uniqueId")
        
        if (uniqueId == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = usersCollection.document(uniqueId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.data?.let { User.fromMap(it, uniqueId) }
                    trySend(user)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun getCurrentUserId(): String? {
        return try {
            val uid = firebaseAuth.currentUser?.uid ?: return null
            // This is synchronous - for async version use getCurrentUser()
            null // Will be resolved asynchronously
        } catch (e: Exception) {
            null
        }
    }
}
